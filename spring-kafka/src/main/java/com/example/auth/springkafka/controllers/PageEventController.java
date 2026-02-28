package com.example.auth.springkafka.controllers;

import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.springkafka.events.PageEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

/**
 * ✅ Controller enrichi :
 * - Documentation OpenAPI (@Operation, @Tag)
 * - Validation des paramètres (@NotBlank, @Validated)
 * - Retour Mono<> pour cohérence avec WebFlux
 */
@RestController
@Validated
@Tag(name = "Page Events", description = "Publication manuelle d'événements de trafic")
public class PageEventController {

    @Autowired
    private StreamBridge streamBridge;

    @GetMapping("/publish")
    @Operation(
            summary = "Publier un événement de page",
            description = "Envoie un PageEvent sur le topic Kafka spécifié",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Événement publié"),
                    @ApiResponse(responseCode = "400", description = "Paramètres invalides")
            }
    )
    public Mono<PageEvent> publish(
            @Parameter(description = "Nom de la page (ex: P1, P2)", example = "P1")
            @RequestParam @NotBlank String name,

            @Parameter(description = "Nom du topic Kafka cible", example = "page-events")
            @RequestParam @NotBlank String topic
    ) {
        PageEvent event = new PageEvent(
                name,
                Math.random() > 0.5 ? "U1" : "U2",
                new Date(),
                10 + new Random().nextInt(10000)
        );
        streamBridge.send(topic, event);
        return Mono.just(event);
    }
}