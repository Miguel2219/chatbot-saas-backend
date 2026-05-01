package com.chatbotsaas.chatbot_saas.quota.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila de consumo del ciclo vigente por tenant, usada en la vista ADMIN.
 * Expuesto por {@code GET /api/quota/admin/all}. Solo visible para ADMIN (doble puerta:
 * permiso {@code dashboard:view} + check {@code isAdmin()} explícito en el service).
 */
@Getter
@Builder
public class QuotaAdminRowDto {

    @JsonProperty(value = "tenant_id")
    private UUID tenantId;

    @JsonProperty(value = "tenant_name")
    private String tenantName;

    @JsonProperty(value = "plan_code")
    private String planCode;

    @JsonProperty(value = "cycle_start")
    private LocalDate cycleStart;

    @JsonProperty(value = "cycle_end")
    private LocalDate cycleEnd;

    @JsonProperty(value = "conversations_count")
    private int conversationsCount;

    @JsonProperty(value = "limit")
    private int limit;

    @JsonProperty(value = "percent_used")
    private double percentUsed;

    @JsonProperty(value = "excess_count")
    private int excessCount;

    @JsonProperty(value = "estimated_excess_cost_cop")
    private int estimatedExcessCostCop;
}
