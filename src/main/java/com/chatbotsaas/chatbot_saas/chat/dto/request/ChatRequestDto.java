package com.chatbotsaas.chatbot_saas.chat.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ChatRequestDto {
    private UUID botId;
    private String sessionId;
    private String message;
}
