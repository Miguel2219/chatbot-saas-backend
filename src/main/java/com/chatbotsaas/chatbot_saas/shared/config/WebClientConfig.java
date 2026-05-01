package com.chatbotsaas.chatbot_saas.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Spring Boot 4 + WebFlux no siempre registra {@link ObjectMapper} como
     * bean por defecto. Lo definimos aquí explícitamente para que pueda
     * inyectarse en {@code WhatsappWebhookController} (deserialización
     * manual del raw body después de validar la firma HMAC).
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json().build();
    }
}
