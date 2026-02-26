package com.chatbotsaas.chatbot_saas.bot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.UUID;

@Getter
public class RegisterBotDto {

    @NotBlank(message = "Name's bot is required")
    @JsonProperty(value = "bot_name")
    private String name;

    @JsonProperty(value = "bot_description")
    @NotBlank(message = "Description is required")
    private String description;

    @JsonProperty(value = "tenant_id")
    private UUID tenantID;

    public RegisterBotDto (String name, String description, UUID tenantID) {
        this.name = name;
        this.description = description;
        this.tenantID = tenantID;
    }
}
