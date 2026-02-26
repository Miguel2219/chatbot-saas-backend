package com.chatbotsaas.chatbot_saas.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDto {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "session_id")
    private String sessionId;

    @JsonProperty(value = "message")
    private String message;
}
