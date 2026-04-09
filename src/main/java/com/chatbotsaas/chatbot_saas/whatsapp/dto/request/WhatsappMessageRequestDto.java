package com.chatbotsaas.chatbot_saas.whatsapp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WhatsappMessageRequestDto {
    @JsonProperty("messaging_product")
    private String messagingProduct = "whatsapp";

    @JsonProperty("to")
    private String to;

    @JsonProperty("type")
    private String type = "text";

    @JsonProperty("text")
    private WhatsappText text;

    @Getter
    @AllArgsConstructor
    public static class WhatsappText {
        @JsonProperty("body")
        private String body;
    }
}
