package com.chatbotsaas.chatbot_saas.bot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;


@Getter
@Builder
public class BotAssigneeDto {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("full_name")
    private String fullName;
}
