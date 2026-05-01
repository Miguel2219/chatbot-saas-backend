package com.chatbotsaas.chatbot_saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body de {@code POST /api/auth/refresh} y {@code POST /api/auth/logout}.
 *
 * Ambos endpoints esperan el mismo campo: el refresh token opaco en claro.
 * El /logout lo acepta aunque no haya un access token adjunto — es la
 * forma de cerrar sesión incluso si el access ya expiró.
 */
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequest {

    @NotBlank
    @JsonProperty("refresh_token")
    private String refreshToken;
}
