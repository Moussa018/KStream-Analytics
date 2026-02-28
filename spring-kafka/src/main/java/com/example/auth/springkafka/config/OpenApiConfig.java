package com.example.auth.springkafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * ✅ Documentation automatique de l'API via Swagger UI.
 * Accessible sur : http://localhost:8081/swagger-ui.html
 *
 * Démontrer cette configuration en entretien montre une maîtrise
 * des standards REST (OpenAPI 3.0).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real-Time Analytics API")
                        .description("API de traitement de flux Kafka Streams avec Server-Sent Events")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Backend Team")
                                .email("contact@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                );
    }
}