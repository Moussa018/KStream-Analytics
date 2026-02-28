package com.example.auth.springkafka.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ FIX : favicon.ico, robots.txt, etc. → 404 silencieux
    // Le navigateur demande toujours favicon.ico, c'est normal.
    // Avant : capturé par handleGeneric() → loggé en ERROR → bruit
    // Maintenant : 404 propre, aucun log
    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<Void>> handleNoResource(NoResourceFoundException ex) {
        return Mono.just(ResponseEntity.notFound().build());
    }

    // ✅ 400 Bad Request (validation, paramètres manquants)
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return Mono.just(ResponseEntity.badRequest().body(
                errorBody(HttpStatus.BAD_REQUEST, ex.getMessage())
        ));
    }

    // ✅ ResponseStatusException — distingue 4xx (WARN) de 5xx (ERROR)
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status != null && status.is4xxClientError()) {
            log.warn("Client error {} : {}", status.value(), ex.getReason());
        } else {
            log.error("Server error {} : {}", ex.getStatusCode().value(), ex.getReason());
        }
        return Mono.just(ResponseEntity
                .status(ex.getStatusCode())
                .body(errorBody(
                        status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
                        ex.getReason() != null ? ex.getReason() : ex.getMessage()
                )));
    }

    // ✅ Fallback — toute exception non gérée ci-dessus
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGeneric(Exception ex) {
        log.error("Erreur interne non gérée: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity.internalServerError().body(
                errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue")
        ));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status",    status.value(),
                "error",     status.getReasonPhrase(),
                "message",   message != null ? message : "N/A"
        );
    }
}