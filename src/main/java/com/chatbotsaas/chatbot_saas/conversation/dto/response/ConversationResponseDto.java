package com.chatbotsaas.chatbot_saas.conversation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
public class ConversationResponseDto {

    @JsonProperty(value = "conversation_id")
    private UUID conversationId;

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "session_id")
    private String sessionId;

    @JsonProperty(value = "role")
    private String role;

    @JsonProperty(value = "message")
    private String message;

    @JsonProperty(value = "created_at")
    private LocalDateTime createdAt;
}
