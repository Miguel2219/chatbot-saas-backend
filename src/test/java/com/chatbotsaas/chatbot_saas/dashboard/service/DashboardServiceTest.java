package com.chatbotsaas.chatbot_saas.dashboard.service;

import com.chatbotsaas.chatbot_saas.dashboard.dto.response.DashboardSummaryDto;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.chatbotsaas.chatbot_saas.lead.repository.LeadRepository;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCurrentDto;
import com.chatbotsaas.chatbot_saas.quota.repository.QuotaSessionDayRepository;
import com.chatbotsaas.chatbot_saas.quota.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock QuotaService quotaService;
    @Mock QuotaSessionDayRepository sessionDayRepo;
    @Mock LeadRepository leadRepo;

    DashboardService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        service = new DashboardService(quotaService, sessionDayRepo, leadRepo);
    }

    @Test
    @DisplayName("Summary típico: mapea QuotaCurrentDto + conteos al DTO agregado")
    void summary_typical_mapsAllFields() {
        LocalDate cs = LocalDate.of(2026, 3, 15);
        LocalDate ce = LocalDate.of(2026, 4, 14);

        when(quotaService.getCurrent(tenantId)).thenReturn(
                QuotaCurrentDto.builder()
                        .tenantId(tenantId)
                        .planCode("BASIC")
                        .cycleStart(cs)
                        .cycleEnd(ce)
                        .conversationsCount(1240)
                        .limit(2000)
                        .percentUsed(62.0)
                        .excessCount(0)
                        .estimatedExcessCostCop(0)
                        .build()
        );
        when(sessionDayRepo.countByTenantIdAndDay(eq(tenantId), any(LocalDate.class))).thenReturn(45L);
        when(leadRepo.countByTenantId(tenantId)).thenReturn(89L);
        when(leadRepo.countByTenantIdAndStatus(tenantId, LeadStatus.PENDING)).thenReturn(7L);

        DashboardSummaryDto dto = service.getSummary(tenantId);

        assertNotNull(dto);
        // conversations_month
        assertEquals(1240, dto.getConversationsMonth().getUsed());
        assertEquals(2000, dto.getConversationsMonth().getLimit());
        assertEquals(62.0, dto.getConversationsMonth().getPercentUsed());
        assertEquals(cs, dto.getConversationsMonth().getCycleStart());
        assertEquals(ce, dto.getConversationsMonth().getCycleEnd());
        // resto
        assertEquals(45L, dto.getConversationsToday());
        assertEquals(89L, dto.getLeadsTotal());
        assertEquals(7L, dto.getLeadsPending());
    }

    @Test
    @DisplayName("Tenant sin ciclo materializado: QuotaService devuelve zero → used/percent en 0 pero límite del plan")
    void summary_tenantWithoutCycleRow_zeroUsage() {
        LocalDate cs = LocalDate.of(2026, 4, 15);
        LocalDate ce = LocalDate.of(2026, 5, 14);

        when(quotaService.getCurrent(tenantId)).thenReturn(
                QuotaCurrentDto.zero(tenantId, cs, ce, 2000, 100, "BASIC")
        );
        when(sessionDayRepo.countByTenantIdAndDay(eq(tenantId), any(LocalDate.class))).thenReturn(0L);
        when(leadRepo.countByTenantId(tenantId)).thenReturn(0L);
        when(leadRepo.countByTenantIdAndStatus(tenantId, LeadStatus.PENDING)).thenReturn(0L);

        DashboardSummaryDto dto = service.getSummary(tenantId);

        assertEquals(0, dto.getConversationsMonth().getUsed());
        assertEquals(2000, dto.getConversationsMonth().getLimit());
        assertEquals(0.0, dto.getConversationsMonth().getPercentUsed());
        assertEquals(cs, dto.getConversationsMonth().getCycleStart());
        assertEquals(ce, dto.getConversationsMonth().getCycleEnd());
        assertEquals(0L, dto.getConversationsToday());
        assertEquals(0L, dto.getLeadsTotal());
        assertEquals(0L, dto.getLeadsPending());
    }

    @Test
    @DisplayName("Tenant sin leads hoy: no hay NPE, los campos retornan 0")
    void summary_noLeadsNoConversations_noNpe() {
        when(quotaService.getCurrent(tenantId)).thenReturn(
                QuotaCurrentDto.zero(tenantId,
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                        2000, 100, "BASIC")
        );
        when(sessionDayRepo.countByTenantIdAndDay(eq(tenantId), any(LocalDate.class))).thenReturn(0L);
        when(leadRepo.countByTenantId(tenantId)).thenReturn(0L);
        when(leadRepo.countByTenantIdAndStatus(tenantId, LeadStatus.PENDING)).thenReturn(0L);

        DashboardSummaryDto dto = service.getSummary(tenantId);

        assertEquals(0L, dto.getConversationsToday());
        assertEquals(0L, dto.getLeadsTotal());
        assertEquals(0L, dto.getLeadsPending());
    }

    @Test
    @DisplayName("Uso por encima del límite: percent > 100 se conserva con redondeo a 1 decimal")
    void summary_percentOverflow_keptWithOneDecimal() {
        LocalDate cs = LocalDate.of(2026, 3, 15);
        LocalDate ce = LocalDate.of(2026, 4, 14);

        when(quotaService.getCurrent(tenantId)).thenReturn(
                QuotaCurrentDto.builder()
                        .tenantId(tenantId)
                        .planCode("BASIC")
                        .cycleStart(cs)
                        .cycleEnd(ce)
                        .conversationsCount(3000)
                        .limit(2000)
                        .percentUsed(150.0)
                        .excessCount(1000)
                        .estimatedExcessCostCop(100_000)
                        .build()
        );
        when(sessionDayRepo.countByTenantIdAndDay(eq(tenantId), any(LocalDate.class))).thenReturn(10L);
        when(leadRepo.countByTenantId(tenantId)).thenReturn(42L);
        when(leadRepo.countByTenantIdAndStatus(tenantId, LeadStatus.PENDING)).thenReturn(5L);

        DashboardSummaryDto dto = service.getSummary(tenantId);

        assertEquals(3000, dto.getConversationsMonth().getUsed());
        assertEquals(2000, dto.getConversationsMonth().getLimit());
        assertEquals(150.0, dto.getConversationsMonth().getPercentUsed());
    }

    @Test
    @DisplayName("Redondeo de percent_used a 1 decimal: 62.47 → 62.5")
    void summary_percentIsRoundedToOneDecimal() {
        when(quotaService.getCurrent(tenantId)).thenReturn(
                QuotaCurrentDto.builder()
                        .tenantId(tenantId)
                        .planCode("BASIC")
                        .cycleStart(LocalDate.of(2026, 3, 15))
                        .cycleEnd(LocalDate.of(2026, 4, 14))
                        .conversationsCount(1249)
                        .limit(2000)
                        .percentUsed(62.47) // valor "sucio" que venga de QuotaService
                        .excessCount(0)
                        .estimatedExcessCostCop(0)
                        .build()
        );
        when(sessionDayRepo.countByTenantIdAndDay(eq(tenantId), any(LocalDate.class))).thenReturn(0L);
        when(leadRepo.countByTenantId(tenantId)).thenReturn(0L);
        when(leadRepo.countByTenantIdAndStatus(tenantId, LeadStatus.PENDING)).thenReturn(0L);

        DashboardSummaryDto dto = service.getSummary(tenantId);

        // round1(62.47) = 62.5
        assertEquals(62.5, dto.getConversationsMonth().getPercentUsed());
    }
}
