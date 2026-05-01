package com.chatbotsaas.chatbot_saas.quota.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Estado actual (ciclo vigente) del consumo de conversaciones de un tenant.
 * <p>
 * Expuesto por {@code GET /api/quota/current}. Si aún no hay fila en
 * {@code tenant_quota_cycles} para el ciclo vigente (primer chat no ha llegado),
 * {@link #zero(UUID, LocalDate, LocalDate, int, int, String)} construye una vista "en cero"
 * con los límites del plan del tenant.
 */
@Getter
@Builder
public class QuotaCurrentDto {

    @JsonProperty(value = "tenant_id")
    private UUID tenantId;

    @JsonProperty(value = "plan_code")
    private String planCode;

    @JsonProperty(value = "cycle_start")
    private LocalDate cycleStart;

    @JsonProperty(value = "cycle_end")
    private LocalDate cycleEnd;

    @JsonProperty(value = "conversations_count")
    private int conversationsCount;

    /** Límite del plan (snapshot del ciclo; congelado al momento de crear la fila). */
    @JsonProperty(value = "limit")
    private int limit;

    /** Porcentaje de uso sobre el límite (0–inf), p.ej. 80.5, 100.0, 175.0. */
    @JsonProperty(value = "percent_used")
    private double percentUsed;

    /** Conversaciones por encima del límite (0 si no se ha excedido). */
    @JsonProperty(value = "excess_count")
    private int excessCount;

    /** Costo estimado del excedente en COP = excessCount * excess_cost_snapshot_cop. */
    @JsonProperty(value = "estimated_excess_cost_cop")
    private int estimatedExcessCostCop;

    /**
     * Construye un DTO "en cero" cuando todavía no existe la fila del ciclo
     * (el primer request del ciclo aún no ha llegado). Se usa en lecturas para que
     * el cliente siempre reciba una respuesta con {@code cycleStart / cycleEnd} válidas.
     */
    public static QuotaCurrentDto zero(
            UUID tenantId,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            int limit,
            int excessUnitCostCop,
            String planCode
    ) {
        return QuotaCurrentDto.builder()
                .tenantId(tenantId)
                .planCode(planCode)
                .cycleStart(cycleStart)
                .cycleEnd(cycleEnd)
                .conversationsCount(0)
                .limit(limit)
                .percentUsed(0.0)
                .excessCount(0)
                .estimatedExcessCostCop(0)
                .build();
    }
}
