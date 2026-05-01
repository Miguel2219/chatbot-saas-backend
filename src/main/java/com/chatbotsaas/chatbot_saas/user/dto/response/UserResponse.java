package com.chatbotsaas.chatbot_saas.user.dto.response;

import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    @JsonProperty(value = "user_id")
    private UUID userId;

    @JsonProperty(value = "must_change_password")
    private Boolean mustChangePassword;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("number_document")
    private String numberDocument;

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("role_ids")
    private List<UUID> roleIds;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;

    @JsonProperty("name")
    private String name;

    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("tenant_name")
    private String tenantName;

    @JsonProperty("lead_assignee_bots")
    private List<UserBotRefDto> leadAssigneeBots;

    @Getter
    @Builder
    public static class UserBotRefDto {
        @JsonProperty("bot_id") private UUID botId;
        @JsonProperty("name")   private String name;
    }
}
