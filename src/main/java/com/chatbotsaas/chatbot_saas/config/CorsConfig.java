package com.chatbotsaas.chatbot_saas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${settings.url.front}")
    private String urlFrontend;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // ── Widget público: cualquier origen, sin credenciales ──
        // Endpoints invocados desde el sitio del cliente final (origin
        // desconocido). DTOs mínimos, sin auth, sin datos sensibles.
        CorsConfiguration widgetConfig = new CorsConfiguration();
        widgetConfig.setAllowedOriginPatterns(List.of("*"));
        widgetConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        widgetConfig.setAllowedHeaders(List.of("Content-Type", "Accept"));
        widgetConfig.setAllowCredentials(false);
        widgetConfig.setMaxAge(1800L);

        // ── Panel privado: dominio real, con credenciales ──
        // Endpoints autenticados con JWT. Whitelist estricta — solo el
        // dominio del panel puede mandar Authorization header.
        CorsConfiguration panelConfig = new CorsConfiguration();
        panelConfig.setAllowedOrigins(List.of(urlFrontend));
        panelConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        panelConfig.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "X-Requested-With"
        ));
        panelConfig.setExposedHeaders(List.of("Location", "X-Total-Count"));
        panelConfig.setAllowCredentials(true);
        panelConfig.setMaxAge(1800L);

        // Orden importa: el patrón más específico primero. Spring resuelve
        // el match más específico al recibir el request.
        source.registerCorsConfiguration("/api/chat", widgetConfig);
        source.registerCorsConfiguration("/api/bot/get_widget_bot/*", widgetConfig);
        source.registerCorsConfiguration("/api/**", panelConfig);

        return source;
    }
}
