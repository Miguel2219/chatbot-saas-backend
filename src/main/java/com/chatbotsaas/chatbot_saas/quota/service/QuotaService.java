package com.chatbotsaas.chatbot_saas.quota.service;

import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaAdminRowDto;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCurrentDto;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCycleHistoryDto;
import com.chatbotsaas.chatbot_saas.quota.entity.SubscriptionPlan;
import com.chatbotsaas.chatbot_saas.quota.entity.TenantQuotaCycle;
import com.chatbotsaas.chatbot_saas.quota.event.QuotaThresholdCrossedEvent;
import com.chatbotsaas.chatbot_saas.quota.repository.QuotaSessionDayRepository;
import com.chatbotsaas.chatbot_saas.quota.repository.TenantQuotaCycleRepository;
import com.chatbotsaas.chatbot_saas.quota.service.QuotaCycleCalculator.CycleWindow;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.util.TimeUtils;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lógica central del sistema de cuotas por ciclo de facturación.
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Registrar una conversación si es la primera vez que se ve la sesión en el día,
 *       dentro del ciclo vigente del tenant.</li>
 *   <li>Hacer rollover lazy del ciclo cuando {@code today > currentCycleEnd}.</li>
 *   <li>Incrementar el contador atómicamente y publicar eventos de umbral una sola vez.</li>
 *   <li>Exponer lecturas para los endpoints {@code /current}, {@code /history}, {@code /admin/all}.</li>
 * </ul>
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final TenantQuotaCycleRepository cycleRepo;
    private final QuotaSessionDayRepository sessionDayRepo;
    private final TenantRepository tenantRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Self-reference proxied — necesaria para que {@code @Transactional(REQUIRES_NEW)} en
     * {@link #createCycleRowWithRaceProtection} funcione cuando se invoca desde
     * {@link #findOrCreateCycleRow}. Sin esto, Spring ignoraría la anotación (bug clásico de
     * self-invocation) y la carrera de rollover no quedaría aislada.
     * <p>
     * {@code @Lazy} rompe el ciclo de dependencias durante la construcción del bean.
     */
    private final QuotaService self;

    public QuotaService(
            TenantQuotaCycleRepository cycleRepo,
            QuotaSessionDayRepository sessionDayRepo,
            TenantRepository tenantRepo,
            ApplicationEventPublisher eventPublisher,
            @Lazy @Autowired QuotaService self
    ) {
        this.cycleRepo = cycleRepo;
        this.sessionDayRepo = sessionDayRepo;
        this.tenantRepo = tenantRepo;
        this.eventPublisher = eventPublisher;
        this.self = self;
    }

    // ------------------------------------------------------------------------
    // Escritura: registrar una conversación (sesión-día única) si es la primera
    // ------------------------------------------------------------------------

    /**
     * Registra una "conversación" (sesión-día única) para el tenant. Si ya se vio esa sesión ese día
     * dentro del ciclo, es no-op. Race-condition safe vía PK compuesta + ON CONFLICT DO NOTHING.
     * <p>
     * Publica {@link QuotaThresholdCrossedEvent} cuando se cruza 80% / 100% / 150% (una vez por ciclo).
     * <p>
     * Debe llamarse dentro de una transacción del flujo de chat; el evento se entrega AFTER_COMMIT.
     */
    @Transactional
    public void registerConversationIfFirst(UUID tenantId, String sessionId) {
        if (tenantId == null || sessionId == null || sessionId.isBlank()) {
            log.warn("registerConversationIfFirst called with invalid args: tenantId={}, sessionId={}", tenantId, sessionId);
            return;
        }

        LocalDate today = TimeUtils.todayBogota();
        CycleSnapshot cycle = ensureCurrentCycle(tenantId, today);

        int inserted = sessionDayRepo.insertIfAbsent(tenantId, cycle.cycleStart, sessionId, today);
        if (inserted == 0) {
            // Ya se había contado esa sesión en ese día para este ciclo — no incrementamos.
            return;
        }

        // Dos round-trips: UPDATE atómico + SELECT del valor post-update.
        // Se separó de un UPDATE ... RETURNING porque Spring Data JPA no soporta @Modifying
        // con query que retorne datos ("Se retornó un resultado cuando no se esperaba ninguno").
        int updated = cycleRepo.increment(cycle.cycleId);
        if (updated == 0) {
            log.error("Failed to increment conversations_count for cycleId={} (no rows affected)", cycle.cycleId);
            return;
        }
        Integer newCount = cycleRepo.findConversationsCountById(cycle.cycleId);
        if (newCount == null) {
            log.error("Failed to read conversations_count for cycleId={} (row missing after increment)", cycle.cycleId);
            return;
        }

        publishThresholdsIfCrossed(cycle, newCount);
    }

    /**
     * Garantiza que el tenant tenga un ciclo vigente cuya ventana contenga {@code today}.
     * Si está desfasado ({@code today > currentCycleEnd}) o nunca se inicializó, avanza el ciclo.
     * Luego busca/crea la fila en {@code tenant_quota_cycles}.
     * <p>
     * Maneja la carrera del rollover (dos hilos intentando crear la misma fila) via try/catch
     * sobre {@link DataIntegrityViolationException}: el que pierde la carrera relee la fila ya creada.
     */
    private CycleSnapshot ensureCurrentCycle(UUID tenantId, LocalDate today) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new AppException("Tenant not found: " + tenantId, HttpStatus.NOT_FOUND));

        Short billingCycleDayBoxed = tenant.getBillingCycleDay();
        if (billingCycleDayBoxed == null) {
            throw new AppException(
                    "Tenant " + tenantId + " has no billing_cycle_day configured",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        int billingCycleDay = billingCycleDayBoxed.intValue();

        LocalDate cs = tenant.getCurrentCycleStart();
        LocalDate ce = tenant.getCurrentCycleEnd();

        boolean needsRollover = cs == null || ce == null || today.isAfter(ce);
        if (needsRollover) {
            CycleWindow w = QuotaCycleCalculator.windowFor(today, billingCycleDay);
            tenant.setCurrentCycleStart(w.start());
            tenant.setCurrentCycleEnd(w.end());
            tenantRepo.save(tenant);
            cs = w.start();
            ce = w.end();
            log.info("Tenant {} rollover: new cycle {}..{}", tenantId, cs, ce);
        }

        SubscriptionPlan plan = tenant.getSubscriptionPlan();
        if (plan == null) {
            throw new AppException(
                    "Tenant " + tenantId + " has no subscription plan assigned",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        TenantQuotaCycle cycle = findOrCreateCycleRow(tenantId, cs, ce, plan);
        return new CycleSnapshot(tenantId, cycle.getId(), cs, ce, cycle.getPlanLimitSnapshot());
    }

    private TenantQuotaCycle findOrCreateCycleRow(
            UUID tenantId,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            SubscriptionPlan plan
    ) {
        return cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart)
                .orElseGet(() -> self.createCycleRowWithRaceProtection(tenantId, cycleStart, cycleEnd, plan));
    }

    /**
     * Crear la fila de ciclo en una sub-transacción independiente (REQUIRES_NEW) para que, si choca
     * contra la UNIQUE {@code uq_tenant_cycle} (otro hilo la creó en el nanosegundo anterior), el rollback
     * del conflicto no arrastre la transacción padre. Tras el conflicto, releemos la fila existente.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantQuotaCycle createCycleRowWithRaceProtection(
            UUID tenantId,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            SubscriptionPlan plan
    ) {
        try {
            TenantQuotaCycle row = TenantQuotaCycle.create(
                    tenantId,
                    cycleStart,
                    cycleEnd,
                    plan.getCycleConversationsLimit(),
                    plan.getExcessConversationCostCop()
            );
            return cycleRepo.saveAndFlush(row);
        } catch (DataIntegrityViolationException e) {
            // Otro hilo ganó la carrera — releer la fila ya creada.
            return cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart)
                    .orElseThrow(() -> new AppException(
                            "Quota cycle row disappeared after unique violation for tenant " + tenantId,
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    private void publishThresholdsIfCrossed(CycleSnapshot cycle, int newCount) {
        int limit = cycle.limit;
        int t80  = (int) Math.ceil(limit * 0.80);
        int t100 = limit;
        int t150 = (int) Math.ceil(limit * 1.50);

        if (newCount >= t80 && cycleRepo.markNotified80(cycle.cycleId) == 1) {
            publishThreshold(cycle, 80, newCount);
        }
        if (newCount >= t100 && cycleRepo.markNotified100(cycle.cycleId) == 1) {
            publishThreshold(cycle, 100, newCount);
        }
        if (newCount >= t150 && cycleRepo.markNotified150(cycle.cycleId) == 1) {
            publishThreshold(cycle, 150, newCount);
        }
    }

    private void publishThreshold(CycleSnapshot cycle, int threshold, int newCount) {
        int excess = Math.max(0, newCount - cycle.limit);
        // Para 80%/100% el excedente puede ser 0; para 150% será limit/2 aprox.
        // El costo en COP usa el snapshot guardado en la fila (no el plan actual).
        int unitCost = fetchExcessCostForCycle(cycle.cycleId);
        int excessCost = excess * unitCost;

        QuotaThresholdCrossedEvent evt = new QuotaThresholdCrossedEvent(
                cycle.tenantId,
                cycle.cycleStart,
                cycle.cycleEnd,
                threshold,
                newCount,
                cycle.limit,
                excess,
                excessCost
        );
        eventPublisher.publishEvent(evt);
    }

    private int fetchExcessCostForCycle(UUID cycleId) {
        return cycleRepo.findById(cycleId)
                .map(TenantQuotaCycle::getExcessCostSnapshotCop)
                .orElse(0);
    }

    // ------------------------------------------------------------------------
    // Lecturas: /current, /history, /admin/all
    // ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public QuotaCurrentDto getCurrent(UUID tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new AppException("Tenant not found: " + tenantId, HttpStatus.NOT_FOUND));

        SubscriptionPlan plan = tenant.getSubscriptionPlan();
        if (plan == null) {
            throw new AppException("Tenant has no plan assigned", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        LocalDate today = TimeUtils.todayBogota();

        // Si el tenant está fuera del ciclo vigente (nadie ha disparado rollover lazy aún),
        // computamos la ventana teórica sin tocar la DB.
        LocalDate csRaw = tenant.getCurrentCycleStart();
        LocalDate ceRaw = tenant.getCurrentCycleEnd();
        final LocalDate cs;
        final LocalDate ce;
        if (csRaw == null || ceRaw == null || today.isAfter(ceRaw)) {
            CycleWindow w = QuotaCycleCalculator.windowFor(today, tenant.getBillingCycleDay().intValue());
            cs = w.start();
            ce = w.end();
        } else {
            cs = csRaw;
            ce = ceRaw;
        }

        int limit = plan.getCycleConversationsLimit();
        int unitCost = plan.getExcessConversationCostCop();

        return cycleRepo.findByTenantIdAndCycleStart(tenantId, cs)
                .map(c -> toCurrentDto(c, plan.getCode()))
                .orElseGet(() -> QuotaCurrentDto.zero(tenantId, cs, ce, limit, unitCost, plan.getCode()));
    }

    @Transactional(readOnly = true)
    public List<QuotaCycleHistoryDto> getHistory(UUID tenantId) {
        if (!tenantRepo.existsById(tenantId)) {
            throw new AppException("Tenant not found: " + tenantId, HttpStatus.NOT_FOUND);
        }
        return cycleRepo.findByTenantIdOrderByCycleStartDesc(tenantId).stream()
                .map(this::toHistoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuotaAdminRowDto> getAllCurrent() {
        LocalDate today = TimeUtils.todayBogota();
        List<Tenant> tenants = tenantRepo.findAll();

        return tenants.stream().map(t -> {
            SubscriptionPlan plan = t.getSubscriptionPlan();
            String planCode = plan != null ? plan.getCode() : "—";
            int limit = plan != null ? plan.getCycleConversationsLimit() : 0;
            int unitCost = plan != null ? plan.getExcessConversationCostCop() : 0;

            LocalDate cs = t.getCurrentCycleStart();
            LocalDate ce = t.getCurrentCycleEnd();
            if (cs == null || ce == null || today.isAfter(ce)) {
                if (t.getBillingCycleDay() != null) {
                    CycleWindow w = QuotaCycleCalculator.windowFor(today, t.getBillingCycleDay().intValue());
                    cs = w.start();
                    ce = w.end();
                }
            }

            TenantQuotaCycle cycle = (cs != null)
                    ? cycleRepo.findByTenantIdAndCycleStart(t.getId(), cs).orElse(null)
                    : null;

            int count = cycle != null ? cycle.getConversationsCount() : 0;
            int effLimit = cycle != null ? cycle.getPlanLimitSnapshot() : limit;
            int effUnitCost = cycle != null ? cycle.getExcessCostSnapshotCop() : unitCost;
            int excess = Math.max(0, count - effLimit);
            int excessCost = excess * effUnitCost;
            double percentUsed = effLimit > 0 ? (count * 100.0 / effLimit) : 0.0;

            return QuotaAdminRowDto.builder()
                    .tenantId(t.getId())
                    .tenantName(t.getName())
                    .planCode(planCode)
                    .cycleStart(cs)
                    .cycleEnd(ce)
                    .conversationsCount(count)
                    .limit(effLimit)
                    .excessCount(excess)
                    .percentUsed(percentUsed)
                    .estimatedExcessCostCop(excessCost)
                    .build();
        }).toList();
    }

    // ------------------------------------------------------------------------
    // Mapping helpers
    // ------------------------------------------------------------------------

    private QuotaCurrentDto toCurrentDto(TenantQuotaCycle c, String planCode) {
        int limit = c.getPlanLimitSnapshot();
        int count = c.getConversationsCount();
        int excess = Math.max(0, count - limit);
        int excessCost = excess * c.getExcessCostSnapshotCop();
        double percentUsed = limit > 0 ? (count * 100.0 / limit) : 0.0;

        return QuotaCurrentDto.builder()
                .tenantId(c.getTenantId())
                .planCode(planCode)
                .cycleStart(c.getCycleStart())
                .cycleEnd(c.getCycleEnd())
                .conversationsCount(count)
                .limit(limit)
                .percentUsed(percentUsed)
                .excessCount(excess)
                .estimatedExcessCostCop(excessCost)
                .build();
    }

    private QuotaCycleHistoryDto toHistoryDto(TenantQuotaCycle c) {
        int limit = c.getPlanLimitSnapshot();
        int count = c.getConversationsCount();
        int excess = Math.max(0, count - limit);
        int excessCost = excess * c.getExcessCostSnapshotCop();
        double percentUsed = limit > 0 ? (count * 100.0 / limit) : 0.0;

        return QuotaCycleHistoryDto.builder()
                .cycleStart(c.getCycleStart())
                .cycleEnd(c.getCycleEnd())
                .conversationsCount(count)
                .limit(limit)
                .percentUsed(percentUsed)
                .excessCount(excess)
                .estimatedExcessCostCop(excessCost)
                .build();
    }

    // ------------------------------------------------------------------------
    // Snapshot interno
    // ------------------------------------------------------------------------

    /**
     * Snapshot del ciclo que estamos actualizando, usado para llevar {@code tenantId} y límites del plan
     * a {@code publishThreshold} sin tener que releerlos.
     */
    private record CycleSnapshot(
            UUID tenantId,
            UUID cycleId,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            int limit
    ) {}
}
