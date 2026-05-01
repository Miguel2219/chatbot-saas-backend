package com.chatbotsaas.chatbot_saas.tenant.dto.response;

import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entrada del selector "TENANT_OWNER" del modal de editar tenant. Lista plana — el frontend
 * marca visualmente al owner actual con el flag {@code is_current_owner}.
 * <p>
 * Incluye {@code notification_channel} para que el modal de Editar Tenant pueda decidir, al
 * cambiar el {@code implementation_type} a uno que exige el campo (WIDGET o BOTH), si el
 * owner ya lo tiene definido o si hay que pedirlo en el formulario antes de permitir guardar.
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class TenantUserSelectDto {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("is_current_owner")
    private boolean isCurrentOwner;

    @JsonProperty("notification_channel")
    private NotificationChannel notificationChannel;
}
