package com.chatbotsaas.chatbot_saas.tenant.dto.request;

import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class UpdateTenantRequestDto {

    @JsonProperty("tenant_name")
    @Size(min = 2, max = 120, message = "Tenant name must be between 2 and 120 characters")
    private String tenantName;

    @JsonProperty("tenant_email")
    @Email(message = "Tenant email must be valid")
    private String tenantEmail;

    @JsonProperty("implementation_type")
    private ImplementationType implementationType;

    /**
     * Si viene, el servicio hace swap: el user indicado pasa a TENANT_OWNER, el actual
     * TENANT_OWNER (si existe y es distinto) pasa a USER. Los roles custom se preservan.
     */
    @JsonProperty("owner_user_id")
    private UUID ownerUserId;

    @JsonProperty("owner_notification_channel")
    private NotificationChannel ownerNotificationChannel;
}
