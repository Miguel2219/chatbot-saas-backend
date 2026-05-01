package com.chatbotsaas.chatbot_saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Respuesta de GET /auth/reset-password/validate.
 *
 * Devuelve el email del user enmascarado (ej: {@code p***o@zolvion.com})
 * para que el formulario de nueva password muestre al user "a quién va a
 * resetear" sin revelar el email completo a un atacante que tenga el link.
 */
@Data
@AllArgsConstructor
public class ValidateResetTokenResponse {

    @JsonProperty("email_masked")
    private String emailMasked;
}
