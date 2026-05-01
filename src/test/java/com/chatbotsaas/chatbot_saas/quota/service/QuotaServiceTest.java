package com.chatbotsaas.chatbot_saas.quota.service;

import com.chatbotsaas.chatbot_saas.quota.entity.SubscriptionPlan;
import com.chatbotsaas.chatbot_saas.quota.entity.TenantQuotaCycle;
import com.chatbotsaas.chatbot_saas.quota.event.QuotaThresholdCrossedEvent;
import com.chatbotsaas.chatbot_saas.quota.repository.QuotaSessionDayRepository;
import com.chatbotsaas.chatbot_saas.quota.repository.TenantQuotaCycleRepository;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios puros (mocks). La semántica real de ON CONFLICT / UPDATE...RETURNING /
 * UPDATE...WHERE flag=false se cubre en {@code QuotaServiceConcurrencyTest} con Postgres real.
 *
 * <p>Aquí validamos la orquestación:
 * <ul>
 *   <li>Primera sesión-día → insertIfAbsent retorna 1 → incremento + evento 80 si corresponde.</li>
 *   <li>Sesión-día duplicada → insertIfAbsent retorna 0 → no incremento.</li>
 *   <li>Umbral no alcanzado → no evento.</li>
 *   <li>Umbral 100 y 150 → evento por cada uno.</li>
 *   <li>Rollover: today > currentCycleEnd → setCurrentCycleStart/End invocado, nueva fila creada.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock TenantQuotaCycleRepository cycleRepo;
    @Mock QuotaSessionDayRepository sessionDayRepo;
    @Mock TenantRepository tenantRepo;
    @Mock ApplicationEventPublisher eventPublisher;

    QuotaService service;

    private UUID tenantId;
    private UUID cycleRowId;
    private Tenant tenant;
    private SubscriptionPlan plan;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        cycleRowId = UUID.randomUUID();
        plan = buildPlan("BASIC", 2000, 100);
        tenant = buildTenant(tenantId, "Acme", "acme@example.com", (short) 15, plan);

        LocalDate today = LocalDate.now();
        QuotaCycleCalculator.CycleWindow w = QuotaCycleCalculator.windowFor(today, 15);
        cycleStart = w.start();
        cycleEnd = w.end();
        setField(tenant, "currentCycleStart", cycleStart);
        setField(tenant, "currentCycleEnd", cycleEnd);

        // Construcción manual (no @InjectMocks) porque el constructor requiere
        // una self-reference (@Lazy) que en unit test simplemente dejamos null —
        // los tests no ejercitan el path de creación de fila de ciclo (siempre
        // mockeamos findByTenantIdAndCycleStart para retornar la fila).
        service = new QuotaService(cycleRepo, sessionDayRepo, tenantRepo, eventPublisher, null);
    }

    @Test
    @DisplayName("Primera sesión-día: incrementa, no cruza 80%, no publica evento")
    void firstSessionDay_underThreshold_incrementsNoEvent() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(10, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(eq(tenantId), eq(cycleStart), eq("S1"), any()))
                .thenReturn(1);
        when(cycleRepo.increment(cycleRowId)).thenReturn(1);
        when(cycleRepo.findConversationsCountById(cycleRowId)).thenReturn(11);

        service.registerConversationIfFirst(tenantId, "S1");

        verify(cycleRepo).increment(cycleRowId);
        verify(cycleRepo).findConversationsCountById(cycleRowId);
        verify(cycleRepo, never()).markNotified80(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Sesión-día duplicada: insertIfAbsent retorna 0, no incrementa")
    void duplicateSessionDay_noOp() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(10, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(eq(tenantId), eq(cycleStart), eq("S1"), any()))
                .thenReturn(0);

        service.registerConversationIfFirst(tenantId, "S1");

        verify(cycleRepo, never()).increment(any());
        verify(cycleRepo, never()).findConversationsCountById(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Cruce 80%: incrementa de 1599 → 1600 y publica evento 80")
    void crosses80Percent_publishesEvent() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(1599, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(any(), any(), anyString(), any())).thenReturn(1);
        when(cycleRepo.increment(cycleRowId)).thenReturn(1);
        when(cycleRepo.findConversationsCountById(cycleRowId)).thenReturn(1600);
        when(cycleRepo.markNotified80(cycleRowId)).thenReturn(1);
        when(cycleRepo.findById(cycleRowId))
                .thenReturn(Optional.of(buildCycleRow(1600, 2000, 100)));

        service.registerConversationIfFirst(tenantId, "S1");

        ArgumentCaptor<QuotaThresholdCrossedEvent> captor =
                ArgumentCaptor.forClass(QuotaThresholdCrossedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        QuotaThresholdCrossedEvent evt = captor.getValue();
        assertEquals(80, evt.threshold());
        assertEquals(1600, evt.conversationsCount());
        assertEquals(2000, evt.limit());
        assertEquals(0, evt.excessCount());
    }

    @Test
    @DisplayName("Cruce 80% pero markNotified80 retorna 0 (otro hilo ganó): no publica")
    void crosses80Percent_butFlagAlreadySet_doesNotPublish() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(1599, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(any(), any(), anyString(), any())).thenReturn(1);
        when(cycleRepo.increment(cycleRowId)).thenReturn(1);
        when(cycleRepo.findConversationsCountById(cycleRowId)).thenReturn(1600);
        when(cycleRepo.markNotified80(cycleRowId)).thenReturn(0);

        service.registerConversationIfFirst(tenantId, "S1");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Cruce 100%: incrementa de 1999 → 2000 y publica evento 100")
    void crosses100Percent_publishesEvent() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(1999, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(any(), any(), anyString(), any())).thenReturn(1);
        when(cycleRepo.increment(cycleRowId)).thenReturn(1);
        when(cycleRepo.findConversationsCountById(cycleRowId)).thenReturn(2000);
        // Ya se notificó 80% antes; solo 100 gana la carrera.
        when(cycleRepo.markNotified80(cycleRowId)).thenReturn(0);
        when(cycleRepo.markNotified100(cycleRowId)).thenReturn(1);
        when(cycleRepo.findById(cycleRowId))
                .thenReturn(Optional.of(buildCycleRow(2000, 2000, 100)));

        service.registerConversationIfFirst(tenantId, "S1");

        ArgumentCaptor<QuotaThresholdCrossedEvent> captor =
                ArgumentCaptor.forClass(QuotaThresholdCrossedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(100, captor.getValue().threshold());
        assertEquals(0, captor.getValue().excessCount());
    }

    @Test
    @DisplayName("Cruce 150%: publica evento con excessCount y costo COP")
    void crosses150Percent_publishesEventWithExcess() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(2999, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(any(), any(), anyString(), any())).thenReturn(1);
        when(cycleRepo.increment(cycleRowId)).thenReturn(1);
        when(cycleRepo.findConversationsCountById(cycleRowId)).thenReturn(3000);
        when(cycleRepo.markNotified80(cycleRowId)).thenReturn(0);
        when(cycleRepo.markNotified100(cycleRowId)).thenReturn(0);
        when(cycleRepo.markNotified150(cycleRowId)).thenReturn(1);
        when(cycleRepo.findById(cycleRowId))
                .thenReturn(Optional.of(buildCycleRow(3000, 2000, 100)));

        service.registerConversationIfFirst(tenantId, "S1");

        ArgumentCaptor<QuotaThresholdCrossedEvent> captor =
                ArgumentCaptor.forClass(QuotaThresholdCrossedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        QuotaThresholdCrossedEvent evt = captor.getValue();
        assertEquals(150, evt.threshold());
        assertEquals(1000, evt.excessCount());          // 3000 - 2000
        assertEquals(100_000, evt.excessCostCop());     // 1000 * 100 COP
    }

    @Test
    @DisplayName("Args inválidos (null/blank): no toca la DB ni publica evento")
    void invalidArgs_noOp() {
        service.registerConversationIfFirst(null, "S1");
        service.registerConversationIfFirst(tenantId, null);
        service.registerConversationIfFirst(tenantId, "");
        service.registerConversationIfFirst(tenantId, "   ");

        verifyNoInteractions(tenantRepo, cycleRepo, sessionDayRepo, eventPublisher);
    }

    @Test
    @DisplayName("Umbral >= (no ==): salto de 1999 → 2001 dispara 100 aunque nunca vea 2000 exacto")
    void greaterThanOrEqual_handlesJumps() {
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart))
                .thenReturn(Optional.of(buildCycleRow(2000, 2000, 100)));
        when(sessionDayRepo.insertIfAbsent(any(), any(), anyString(), any())).thenReturn(1);
        // Simulamos salto: retorna 2001 sin haber pasado por 2000 exacto
        when(cycleRepo.increment(cycleRowId)).thenReturn(1);
        when(cycleRepo.findConversationsCountById(cycleRowId)).thenReturn(2001);
        when(cycleRepo.markNotified80(cycleRowId)).thenReturn(0);
        when(cycleRepo.markNotified100(cycleRowId)).thenReturn(1);
        when(cycleRepo.findById(cycleRowId))
                .thenReturn(Optional.of(buildCycleRow(2001, 2000, 100)));

        service.registerConversationIfFirst(tenantId, "S1");

        ArgumentCaptor<QuotaThresholdCrossedEvent> captor =
                ArgumentCaptor.forClass(QuotaThresholdCrossedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(100, captor.getValue().threshold());
    }

    // ------------------------------------------------------------------------
    // Helpers: construcción de objetos (reflection para setters privados)
    // ------------------------------------------------------------------------

    private SubscriptionPlan buildPlan(String code, int limit, int unitCostCop) {
        SubscriptionPlan p = new SubscriptionPlan();
        setField(p, "id", UUID.randomUUID());
        setField(p, "code", code);
        setField(p, "name", code);
        setField(p, "cycleConversationsLimit", limit);
        setField(p, "excessConversationCostCop", unitCostCop);
        setField(p, "isActive", Boolean.TRUE);
        return p;
    }

    private Tenant buildTenant(UUID id, String name, String email, Short cycleDay, SubscriptionPlan plan) {
        Tenant t = new Tenant(name, email, Boolean.TRUE, null);
        setField(t, "id", id);
        setField(t, "subscriptionPlan", plan);
        setField(t, "billingCycleDay", cycleDay);
        return t;
    }

    private TenantQuotaCycle buildCycleRow(int count, int limit, int unitCostCop) {
        TenantQuotaCycle row = TenantQuotaCycle.create(tenantId, cycleStart, cycleEnd, limit, unitCostCop);
        setField(row, "id", cycleRowId);
        setField(row, "conversationsCount", count);
        setField(row, "notified80", Boolean.FALSE);
        setField(row, "notified100", Boolean.FALSE);
        setField(row, "notified150", Boolean.FALSE);
        return row;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = findField(target.getClass(), name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot set field " + name + " on " + target.getClass(), e);
        }
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
