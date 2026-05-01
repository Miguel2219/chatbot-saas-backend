package com.chatbotsaas.chatbot_saas.tenant.dto.request;

import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class CreateTenantRequestDto {

    // ─── Tenant ─────────────────────────────────────────────────────────────
    @JsonProperty("tenant_name")
    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 120, message = "Tenant name must be between 2 and 120 characters")
    private String tenantName;

    @JsonProperty("tenant_email")
    @NotBlank(message = "Tenant email is required")
    @Email(message = "Tenant email must be valid")
    private String tenantEmail;

    @JsonProperty("implementation_type")
    @NotNull(message = "Implementation type is required")
    private ImplementationType implementationType;

    // ─── Owner (se crea junto al tenant; se promueve a TENANT_OWNER) ────────
    @JsonProperty("owner_email")
    @NotBlank(message = "Owner email is required")
    @Email(message = "Owner email must be valid")
    private String ownerEmail;

    @JsonProperty("owner_name")
    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name must be at most 100 characters")
    private String ownerName;

    @JsonProperty("owner_lastname")
    @NotBlank(message = "Owner lastname is required")
    @Size(max = 100, message = "Owner lastname must be at most 100 characters")
    private String ownerLastname;

    @JsonProperty("owner_phone")
    @Size(max = 30, message = "Owner phone must be at most 30 characters")
    private String ownerPhone;

    @JsonProperty("owner_number_document")
    @Size(max = 30, message = "Owner document number must be at most 30 characters")
    private String ownerNumberDocument;

    @JsonProperty("owner_notification_channel")
    private NotificationChannel ownerNotificationChannel;
}
