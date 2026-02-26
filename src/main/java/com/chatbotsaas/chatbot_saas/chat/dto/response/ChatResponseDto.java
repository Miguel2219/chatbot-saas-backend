package com.chatbotsaas.chatbot_saas.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponseDto {
    private String sessionId;
    private String response;
}
