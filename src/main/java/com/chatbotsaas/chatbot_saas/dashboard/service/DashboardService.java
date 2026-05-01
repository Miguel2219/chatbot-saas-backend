package com.chatbotsaas.chatbot_saas.dashboard.service;

import com.chatbotsaas.chatbot_saas.dashboard.dto.response.DashboardSummaryDto;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.chatbotsaas.chatbot_saas.lead.repository.LeadRepository;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCurrentDto;
import com.chatbotsaas.chatbot_saas.quota.repository.QuotaSessionDayRepository;
import com.chatbotsaas.chatbot_saas.quota.service.QuotaService;
import com.chatbotsaas.chatbot_saas.shared.util.TimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DashboardService {

    private final QuotaService quotaService;
    private final QuotaSessionDayRepository quotaSessionDayRepo;
    private final LeadRepository leadRepo;

    public DashboardService(
            QuotaService quotaService,
            QuotaSessionDayRepository quotaSessionDayRepo,
            LeadRepository leadRepo
    ) {
        this.quotaService = quotaService;
        this.quotaSessionDayRepo = quotaSessionDayRepo;
        this.leadRepo = leadRepo;
    }

    /**
     * Construye el resumen del Dashboard para el tenant solicitado.
     * <p>
     * El tenant scoping ya fue resuelto por el controlador — aquí no se validan permisos.
     * Llamar con un {@code tenantId} inválido propagará la excepción del servicio interno
     * (p.ej. {@link QuotaService#getCurrent(UUID)} lanza 404 si el tenant no existe).
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(UUID tenantId) {
        // 1) Conversaciones del mes — reutilizamos todo el cálculo del módulo quotas.
        QuotaCurrentDto current = quotaService.getCurrent(tenantId);

        DashboardSummaryDto.ConversationsMonthDto conversationsMonth =
                DashboardSummaryDto.ConversationsMonthDto.builder()
                        .used(current.getConversationsCount())
                        .limit(current.getLimit())
                        .percentUsed(round1(current.getPercentUsed()))
                        .cycleStart(current.getCycleStart())
                        .cycleEnd(current.getCycleEnd())
                        .build();

        // 2) Conversaciones hoy — fuente: quota_session_days (misma definición que el módulo quotas).
        LocalDate today = TimeUtils.todayBogota();
        long conversationsToday = quotaSessionDayRepo.countByTenantIdAndDay(tenantId, today);

        // 3) y 4) Leads — vía subquery sobre Bot (Lead no tiene tenantId directo).
        long leadsTotal = leadRepo.countByTenantId(tenantId);
        long leadsPending = leadRepo.countByTenantIdAndStatus(tenantId, LeadStatus.PENDING);

        return DashboardSummaryDto.builder()
                .conversationsMonth(conversationsMonth)
                .conversationsToday(conversationsToday)
                .leadsTotal(leadsTotal)
                .leadsPending(leadsPending)
                .build();
    }

    /** Redondea a 1 decimal (p.ej. 62.47 → 62.5). El frontend formatea; acá solo evitamos ruido. */
    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
