package com.chatbotsaas.chatbot_saas.bot.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SystemPromptDto {

    @JsonProperty("system_prompt")
    private String systemPrompt;
}

