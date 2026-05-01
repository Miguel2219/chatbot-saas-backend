package com.chatbotsaas.chatbot_saas.quota.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento publicado por {@code QuotaService} cuando el contador del ciclo de un tenant
 * cruza un umbral (80%, 100% o 150%) por primera vez en ese ciclo.
 * <p>
 * La atomicidad del "una sola vez por ciclo" está garantizada por el
 * {@code UPDATE ... WHERE flag = false} en {@code tenant_quota_cycles}:
 * solo el hilo que logra marcar el flag publica el evento.
 * <p>
 * Se consume por {@code QuotaThresholdEmailListener} via
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} para no notificar si la
 * transacción del chat hizo rollback, y {@code @Async} para no bloquear el flujo.
 *
 * @param tenantId           tenant afectado
 * @param cycleStart         inicio del ciclo (inclusive, Bogotá)
 * @param cycleEnd           fin del ciclo (inclusive, Bogotá)
 * @param threshold          80, 100 o 150 (porcentaje del límite cruzado)
 * @param conversationsCount contador después del incremento
 * @param limit              límite de conversaciones del plan (snapshot del ciclo)
 * @param excessCount        conversaciones sobre el límite (0 para 80%/100%, positivo para 150%)
 * @param excessCostCop      costo estimado del excedente = excessCount * snapshot_cost_cop
 */
public record QuotaThresholdCrossedEvent(
        UUID tenantId,
        LocalDate cycleStart,
        LocalDate cycleEnd,
        int threshold,
        int conversationsCount,
        int limit,
        int excessCount,
        int excessCostCop
) {}
