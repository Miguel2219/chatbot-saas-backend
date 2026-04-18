package com.chatbotsaas.chatbot_saas.bot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ResponseBotSelectDto {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "name")
    private String name;
}
