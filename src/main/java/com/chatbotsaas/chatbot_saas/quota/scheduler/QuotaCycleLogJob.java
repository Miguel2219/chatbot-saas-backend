package com.chatbotsaas.chatbot_saas.quota.scheduler;

import com.chatbotsaas.chatbot_saas.shared.util.TimeUtils;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Job diario de <b>observabilidad</b> — <i>NO</i> lógica de negocio.
 * <p>
 * A las 00:05 Bogotá recorre los tenants cuyo {@code currentCycleEnd} ya venció y loguea
 * un INFO por cada uno. El rollover real lo hace {@code QuotaService.ensureCurrentCycle}
 * de forma lazy cuando llega el primer mensaje del nuevo ciclo.
 * <p>
 * Este job existe para darle visibilidad al equipo sobre quién cruzó ciclo hoy sin
 * tocar la base de datos — si ningún tenant manda chat hoy, simplemente nadie hace
 * rollover y el log le recuerda que están "pendientes de materializar".
 */
@Component
public class QuotaCycleLogJob {

    private static final Logger log = LoggerFactory.getLogger(QuotaCycleLogJob.class);

    private final TenantRepository tenantRepo;

    public QuotaCycleLogJob(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
    public void logTenantsStartingNewCycleToday() {
        LocalDate today = TimeUtils.todayBogota();
        List<Tenant> crossing = tenantRepo.findByCurrentCycleEndBefore(today);

        if (crossing.isEmpty()) {
            log.debug("QuotaCycleLogJob: no tenants crossing cycle today={}", today);
            return;
        }

        log.info("QuotaCycleLogJob: {} tenant(s) with expired cycle (lazy rollover pending) on {}",
                crossing.size(), today);
        crossing.forEach(t ->
                log.info(
                        "Tenant {} ({}): cycle {}..{} has ended; next cycle will be materialized lazily on first request.",
                        t.getId(),
                        t.getName(),
                        t.getCurrentCycleStart(),
                        t.getCurrentCycleEnd()
                )
        );
    }
}
