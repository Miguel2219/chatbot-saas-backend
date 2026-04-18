package com.chatbotsaas.chatbot_saas.bot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ResponseBotTableDto {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "is_active")
    private Boolean isActive;

    @JsonProperty(value = "created_at")
    private LocalDateTime createdAt;
}
