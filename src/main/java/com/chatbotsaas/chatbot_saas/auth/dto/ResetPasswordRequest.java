package com.chatbotsaas.chatbot_saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body de POST /auth/reset-password.
 *
 * {@code token} es el string raw que venía en la URL del email. El
 * {@code newPassword} se valida acá solo en tamaño mínimo — la regla
 * completa (letra + número) la aplica el service, que devuelve
 * {@code 400 WEAK_PASSWORD} si falla.
 */
@Data
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8)
    @JsonProperty("new_password")
    private String newPassword;
}
