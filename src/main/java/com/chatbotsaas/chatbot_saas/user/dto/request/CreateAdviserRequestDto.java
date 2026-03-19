package com.chatbotsaas.chatbot_saas.user.dto.request;

import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateAdviserRequestDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("number_document")
    private String numberDocument;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;

    @JsonProperty("bot_ids")
    private List<UUID> botIds;

}
