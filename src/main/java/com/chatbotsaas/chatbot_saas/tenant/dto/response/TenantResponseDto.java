package com.chatbotsaas.chatbot_saas.tenant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResponseDto {

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("implementation_type")
    private String implementationType;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}