package com.chatbotsaas.chatbot_saas.bot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateSystemPromptDto {

    @JsonProperty("system_prompt")
    @NotNull(message = "system_prompt is required")
    @Size(max = 20000, message = "system_prompt must be at most 20000 characters")
    private String systemPrompt;
}
