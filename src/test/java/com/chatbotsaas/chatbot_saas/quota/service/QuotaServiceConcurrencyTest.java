package com.chatbotsaas.chatbot_saas.quota.service;

import com.chatbotsaas.chatbot_saas.quota.entity.SubscriptionPlan;
import com.chatbotsaas.chatbot_saas.quota.entity.TenantQuotaCycle;
import com.chatbotsaas.chatbot_saas.quota.repository.QuotaSessionDayRepository;
import com.chatbotsaas.chatbot_saas.quota.repository.SubscriptionPlanRepository;
import com.chatbotsaas.chatbot_saas.quota.repository.TenantQuotaCycleRepository;
import com.chatbotsaas.chatbot_saas.shared.util.TimeUtils;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.resend.Resend;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de concurrencia con PostgreSQL real (Testcontainers).
 * <p>
 * H2 no refleja la semántica exacta de:
 * <ul>
 *   <li>{@code INSERT ... ON CONFLICT DO NOTHING} (no existe en H2 sin dialecto custom)</li>
 *   <li>{@code UPDATE ... RETURNING} (Postgres-specific)</li>
 *   <li>Violación de UNIQUE constraint y recuperación de la tx padre con REQUIRES_NEW</li>
 * </ul>
 * Por eso estos tests usan Postgres real; ejecutar con Docker corriendo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotaServiceConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("chatbot_saas_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    // Mockeamos el mailer para que el listener AFTER_COMMIT no intente enviar correo real.
    @MockitoBean
    Resend resend;

    @Autowired QuotaService quotaService;
    @Autowired TenantRepository tenantRepo;
    @Autowired SubscriptionPlanRepository planRepo;
    @Autowired TenantQuotaCycleRepository cycleRepo;
    @Autowired QuotaSessionDayRepository sessionDayRepo;

    @PersistenceContext EntityManager em;

    // ------------------------------------------------------------------------
    // Test 1: misma sesión, 10 hilos → contador = 1
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("10 hilos con la misma sessionId en el mismo día → conversations_count = 1")
    void sameSession10Threads_countsOnlyOnce() throws Exception {
        LocalDate today = TimeUtils.todayBogota();
        SubscriptionPlan plan = seedBasicPlan();
        Tenant tenant = seedTenant(plan, (short) 15, today.minusDays(5), today.plusDays(25));
        UUID tenantId = tenant.getId();
        LocalDate cycleStart = tenant.getCurrentCycleStart();

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        var futures = IntStream.range(0, threads)
                .mapToObj(i -> pool.submit((Runnable) () -> {
                    try {
                        start.await();
                        quotaService.registerConversationIfFirst(tenantId, "same-session");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                }))
                .toList();

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "threads did not finish in time");
        for (var f : futures) f.get();
        pool.shutdown();

        TenantQuotaCycle cycle = cycleRepo.findByTenantIdAndCycleStart(tenantId, cycleStart)
                .orElseThrow();
        assertEquals(1, cycle.getConversationsCount(),
                "Con la misma sessionId en el mismo día, solo una conversación debería contarse.");
        assertEquals(1L,
                sessionDayRepo.countByTenantIdAndCycleStartAndSessionId(tenantId, cycleStart, "same-session"),
                "Exactamente una fila en quota_session_days para esa sessionId.");
    }

    // ------------------------------------------------------------------------
    // Test 2: rollover concurrente al cruzar de ciclo
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("10 hilos al iniciar ciclo nuevo → 1 fila nueva en tenant_quota_cycles + 10 conversaciones")
    void concurrentRolloverFirstMillisecondOfNewCycle() throws Exception {
        LocalDate today = TimeUtils.todayBogota();
        SubscriptionPlan plan = seedBasicPlan();

        // Forzar: currentCycleEnd = ayer → today > currentCycleEnd → rollover necesario.
        // Elegimos billingCycleDay estable. Luego leemos la ventana esperada con el calculator.
        short billingCycleDay = (short) Math.min(today.getDayOfMonth(), 28);
        Tenant tenant = seedTenant(plan, billingCycleDay, today.minusMonths(1), today.minusDays(1));
        UUID tenantId = tenant.getId();

        QuotaCycleCalculator.CycleWindow expected =
                QuotaCycleCalculator.windowFor(today, billingCycleDay);
        LocalDate expectedStart = expected.start();

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        var futures = IntStream.range(0, threads)
                .mapToObj(i -> pool.submit((Runnable) () -> {
                    try {
                        start.await();
                        quotaService.registerConversationIfFirst(tenantId, "S-" + i);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                }))
                .toList();

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "threads did not finish in time");
        for (var f : futures) f.get();
        pool.shutdown();

        // Assert 1: exactamente 1 fila en tenant_quota_cycles con expectedStart.
        List<TenantQuotaCycle> rows = findAllCyclesFor(tenantId, expectedStart);
        assertEquals(1, rows.size(),
                "Bajo carrera de rollover, UNIQUE (tenant_id, cycle_start) + REQUIRES_NEW debe garantizar una sola fila.");

        // Assert 2: las 10 conversaciones cayeron en el ciclo nuevo.
        TenantQuotaCycle newCycle = rows.get(0);
        assertEquals(threads, newCycle.getConversationsCount(),
                "Las 10 conversaciones deben contarse contra el ciclo nuevo.");
        assertEquals(threads,
                sessionDayRepo.countByTenantIdAndCycleStart(tenantId, expectedStart),
                "10 filas distintas en quota_session_days (una por sessionId).");

        // Assert 3: el tenant tiene currentCycleStart/end del nuevo período.
        Tenant refreshed = tenantRepo.findById(tenantId).orElseThrow();
        assertEquals(expectedStart, refreshed.getCurrentCycleStart());
        assertEquals(expected.end(), refreshed.getCurrentCycleEnd());
    }

    // ------------------------------------------------------------------------
    // Seeders
    // ------------------------------------------------------------------------

    @Transactional
    SubscriptionPlan seedBasicPlan() {
        return planRepo.findByCode("BASIC").orElseGet(() -> {
            SubscriptionPlan p = new SubscriptionPlan();
            setField(p, "code", "BASIC");
            setField(p, "name", "Plan Básico");
            setField(p, "cycleConversationsLimit", 2000);
            setField(p, "excessConversationCostCop", 100);
            setField(p, "isActive", Boolean.TRUE);
            return planRepo.saveAndFlush(p);
        });
    }

    @Transactional
    Tenant seedTenant(SubscriptionPlan plan, short billingCycleDay,
                              LocalDate cycleStart, LocalDate cycleEnd) {
        Tenant t = new Tenant("Test " + UUID.randomUUID(),
                "tenant-" + UUID.randomUUID() + "@example.test",
                Boolean.TRUE, null);
        t.setSubscriptionPlan(plan);
        t.setBillingCycleDay(billingCycleDay);
        t.setCurrentCycleStart(cycleStart);
        t.setCurrentCycleEnd(cycleEnd);
        tenantRepo.saveAndFlush(t);
        return t;
    }

    // Consulta manual porque el repo solo tiene findByTenantIdAndCycleStart → Optional;
    // queremos List para aserciones del test.
    private List<TenantQuotaCycle> findAllCyclesFor(UUID tenantId, LocalDate cycleStart) {
        return em.createQuery(
                        "SELECT c FROM TenantQuotaCycle c WHERE c.tenantId = :tid AND c.cycleStart = :cs",
                        TenantQuotaCycle.class)
                .setParameter("tid", tenantId)
                .setParameter("cs", cycleStart)
                .getResultList();
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = findField(target.getClass(), name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
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
