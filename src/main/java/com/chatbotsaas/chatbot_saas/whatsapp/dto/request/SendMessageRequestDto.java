package com.chatbotsaas.chatbot_saas.whatsapp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SendMessageRequestDto {

    /** Siempre "whatsapp" — Meta lo exige en TODOS los outbound. */
    @JsonProperty("messaging_product")
    private String messagingProduct;

    /**
     * Tipo de destinatario. Meta acepta "individual" para chats 1:1
     * (caso de uso actual). Opcional pero recomendado.
     */
    @JsonProperty("recipient_type")
    private String recipientType;

    /** Número del destinatario, sin "+". */
    @JsonProperty("to")
    private String to;

    /** "text" para mensajes de texto plano. */
    @JsonProperty("type")
    private String type;

    @JsonProperty("text")
    private TextBody text;

    @AllArgsConstructor
    @Getter
    public static class TextBody {
        @JsonProperty("body")
        private String body;
    }
}
