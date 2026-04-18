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

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;

    @JsonProperty("name")
    private String name;

    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("tenant_name")
    private String tenantName;

}
