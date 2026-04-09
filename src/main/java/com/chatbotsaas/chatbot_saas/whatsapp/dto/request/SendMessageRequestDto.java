package com.chatbotsaas.chatbot_saas.whatsapp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SendMessageRequestDto {

    @JsonProperty("messaging_product")
    private String messagingProduct;

    @JsonProperty("to")
    private String to;

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
