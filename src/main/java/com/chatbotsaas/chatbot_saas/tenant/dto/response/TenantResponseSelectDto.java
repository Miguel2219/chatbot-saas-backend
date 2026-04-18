package com.chatbotsaas.chatbot_saas.tenant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TenantResponseSelectDto {

    @JsonProperty("tenant_id")
    private UUID tenantId;

    @JsonProperty("name")
    private String name;
}
