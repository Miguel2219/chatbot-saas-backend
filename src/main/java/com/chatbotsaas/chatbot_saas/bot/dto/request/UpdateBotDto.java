package com.chatbotsaas.chatbot_saas.bot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class UpdateBotDto {

    @JsonProperty("bot_name")
    @Size(min = 1, max = 120, message = "Name must be between 1 and 120 characters")
    private String name;

    @JsonProperty("bot_description")
    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
    private String description;

    @JsonProperty("lead_assignee_user_ids")
    private List<UUID> leadAssigneeUserIds;
}
