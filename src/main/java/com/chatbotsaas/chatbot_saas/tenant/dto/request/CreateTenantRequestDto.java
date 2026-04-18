package com.chatbotsaas.chatbot_saas.tenant.dto.request;

import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CreateTenantRequestDto {

    @JsonProperty("tenant_name")
    private String tenantName;

    @JsonProperty("tenant_email")
    private String tenantEmail;

    @JsonProperty("implementation_type")
    private ImplementationType implementationType;

    @JsonProperty("email_user")
    private String emailUser;

    @JsonProperty("name")
    private String name;

    @JsonProperty("lastname")
    private String lastname;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("number_document")
    private String numberDocument;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;

    @JsonProperty("role_ids")
    private List<UUID> roleIds;
}
