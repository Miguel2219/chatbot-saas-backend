package com.chatbotsaas.chatbot_saas.user.dto.response;

import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AdviserResponseDto {
    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;

    @JsonProperty("assigned_bots")
    private List<String> assignedBots;
}
