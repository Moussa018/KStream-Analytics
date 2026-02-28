package com.example.auth.springkafka.handlers;

import java.time.Duration;
import java.time.Instant;
import java.util.Date; 
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.springkafka.events.PageEvent;

import reactor.core.publisher.Flux;


@Component
@RestController
public class PageEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PageEventHandler.class);
    private static final String COUNT_STORE = "count-store";
    private static final long MIN_DURATION_MS = 100L;

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private InteractiveQueryService interactiveQueryService;

    /**
     * Consomme les PageEvents depuis le topic page-events (logging).
     */
    @Bean
    public Consumer<PageEvent> pageEventConsumer() {
        return event -> log.debug("Event reçu : {}", event);
    }

    /**
     * Produit un PageEvent aléatoire chaque seconde vers page-events.
     */
    @Bean
    public Supplier<PageEvent> pageEventSupplier() {
        return () -> new PageEvent(
                Math.random() > 0.5 ? "P1" : "P2",
                Math.random() > 0.5 ? "U1" : "U2",
                new Date(),
                new Random().nextInt(10000)
        );
    }

    /**
     * Kafka Streams pipeline :
     * 1. Filtre les sessions trop courtes (< 100ms)
     * 2. Regroupe par nom de page
     * 3. Compte sur fenêtres glissantes de 5s
     * 4. Matérialise dans un store interrogeable
     */
    @Bean
    public Function<KStream<String, PageEvent>, KStream<String, Long>> kStreamKStreamFunction() {
        return input -> input
                .filter((k, v) -> v != null && v.duration() > MIN_DURATION_MS)
                .map((k, v) -> new KeyValue<>(v.name(), v.duration()))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(
                        TimeWindows.ofSizeAndGrace(
                                Duration.ofSeconds(5),
                                Duration.ofSeconds(1)
                        )
                )
                //BUGFIX : Bytes importé correctement
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(COUNT_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .map((k, v) -> new KeyValue<>(k.key(), v));
    }

    /**
     * Endpoint SSE : pousse les statistiques de trafic en temps réel.
     * Gestion d'erreur : retourne une map vide si le store n'est pas encore prêt.
     */
    @GetMapping(path = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> {
                    Map<String, Long> stats = new HashMap<>();
                    // try-catch si le store KStream n'est pas encore initialisé
                    try {
                        ReadOnlyWindowStore<String, Long> windowStore =
                                interactiveQueryService.getQueryableStore(
                                        COUNT_STORE,
                                        QueryableStoreTypes.windowStore()
                                );
                        Instant now = Instant.now();
                        Instant from = now.minusMillis(5000);

                        try (KeyValueIterator<Windowed<String>, Long> iterator =
                                     windowStore.fetchAll(from, now)) {
                            // Try-with-resources pour fermer l'itérateur proprement
                            while (iterator.hasNext()) {
                                KeyValue<Windowed<String>, Long> next = iterator.next();
                                stats.merge(next.key.key(), next.value, Long::sum);
                            }
                        }
                    } catch (Exception e) {
                        // Le store peut ne pas être prêt au démarrage
                        log.warn("Store '{}' pas encore disponible : {}", COUNT_STORE, e.getMessage());
                    }
                    return stats;
                })
                .onErrorContinue((err, obj) ->
                        log.error("Erreur dans le flux analytics : {}", err.getMessage())
                );
    }
}