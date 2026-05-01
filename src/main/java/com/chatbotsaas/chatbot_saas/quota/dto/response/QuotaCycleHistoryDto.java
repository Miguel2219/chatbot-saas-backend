package com.chatbotsaas.chatbot_saas.quota.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Fila de histórico de un ciclo cerrado (o el ciclo vigente) para un tenant.
 * Expuesto por {@code GET /api/quota/history}. Ordenado {@code cycle_start DESC}
 * en el service; no requiere paginación (pocos ciclos por tenant).
 */
@Getter
@Builder
public class QuotaCycleHistoryDto {

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
