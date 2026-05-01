package com.chatbotsaas.chatbot_saas.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DashboardSummaryDto {

    @JsonProperty("conversations_month")
    private ConversationsMonthDto conversationsMonth;

    @JsonProperty("conversations_today")
    private long conversationsToday;

    @JsonProperty("leads_total")
    private long leadsTotal;

    @JsonProperty("leads_pending")
    private long leadsPending;

    /**
     * Sub-DTO con el consumo del ciclo de facturación vigente.
     * Espeja los campos relevantes de {@link com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCurrentDto}
     * pero sólo los que el Dashboard necesita (el módulo "quotas" expone el DTO completo).
     */
    @Getter
    @Builder
    public static class ConversationsMonthDto {

        @JsonProperty("used")
        private int used;

        @JsonProperty("limit")
        private int limit;

        /** Porcentaje de uso, 1 decimal (p.ej. 62.0, 80.5, 100.0). */
        @JsonProperty("percent_used")
        private double percentUsed;

        @JsonProperty("cycle_start")
        private LocalDate cycleStart;

        @JsonProperty("cycle_end")
        private LocalDate cycleEnd;
    }
}
