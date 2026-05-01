package com.chatbotsaas.chatbot_saas.user.dto.request;

import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class CreateUserRequestDto {

    @JsonProperty("email")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @JsonProperty("name")
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @JsonProperty("lastname")
    @NotBlank(message = "Lastname is required")
    @Size(max = 100, message = "Lastname must be at most 100 characters")
    private String lastname;

    @JsonProperty("phone")
    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @JsonProperty("number_document")
    @Size(max = 30, message = "Document number must be at most 30 characters")
    private String numberDocument;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;

    @JsonProperty("role_ids")
    private List<UUID> roleIds;

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("lead_assignee_bot_ids")
    private List<UUID> leadAssigneeBotIds;
}
