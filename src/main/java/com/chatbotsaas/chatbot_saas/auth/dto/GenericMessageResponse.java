package com.chatbotsaas.chatbot_saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Respuesta 200 con un único campo {@code message}. Se usa para endpoints
 * que solo necesitan confirmar éxito con texto human-friendly — POST
 * /auth/forgot-password y POST /auth/reset-password.
 */
@Data
@AllArgsConstructor
public class GenericMessageResponse {

    @JsonProperty("message")
    private String message;
}
