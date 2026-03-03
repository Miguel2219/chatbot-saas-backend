package com.chatbotsaas.chatbot_saas.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponseDto {
    @JsonProperty(value = "session_id")
    private String sessionId;

    @JsonProperty(value = "response")
    private String response;
}
