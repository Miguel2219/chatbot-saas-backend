package com.chatbotsaas.chatbot_saas.bot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ResponseBotDto {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "is_active")
    private Boolean isActive;

    @JsonProperty(value = "implementation_type")
    private String implementationType;

    @JsonProperty(value = "lead_assignees")
    private List<BotAssigneeDto> leadAssignees;
}
