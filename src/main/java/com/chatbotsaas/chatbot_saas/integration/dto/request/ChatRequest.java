package com.chatbotsaas.chatbot_saas.integration.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ChatRequest {
    private UUID botId;
    private String sessionId;
    private String message;
}
