package com.chatbotsaas.chatbot_saas.integration.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ChatRequest {
    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "session_id")
    private String sessionId;

    @JsonProperty(value = "message")
    private String message;
}
