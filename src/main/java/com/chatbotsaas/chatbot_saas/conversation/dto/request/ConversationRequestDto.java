package com.chatbotsaas.chatbot_saas.conversation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class ConversationRequestDto {

    @JsonProperty(value = "bot_id")
    @NotNull
    private UUID botId;

    @NotNull
    @JsonProperty(value = "session_id")
    private String sessionId;

    @NotNull
    @NotBlank
    @JsonProperty(value = "role")
    private String role;

    @NotBlank
    @JsonProperty(value = "message")
    private String message;
}
