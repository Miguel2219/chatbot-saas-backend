package com.chatbotsaas.chatbot_saas.quota.service;

import java.time.LocalDate;

/**
 * Lógica pura para el cálculo del ciclo de facturación de un tenant.
 * <p>
 * Dado un día de corte ({@code billingCycleDay}, 1..28) y una fecha "hoy", devuelve la ventana
 * [start, end] (ambos inclusive) del ciclo vigente. Cap en 28 evita los problemas de los días
 * 29/30/31 que no existen en todos los meses (ej. febrero).
 * <p>
 * Esta clase no depende de Spring ni de ningún componente externo — es fácil de unit-testear
 * y se llama desde {@link QuotaService} y desde la lógica SQL de la migración V3 (que espeja
 * esta misma fórmula en Postgres).
 */
public final class QuotaCycleCalculator {

    private QuotaCycleCalculator() {}

    public static int clampCycleDay(int d) {
        if (d < 1) return 1;
        if (d > 28) return 28;
        return d;
    }

    /**
     * Ventana del ciclo vigente para {@code today} según {@code billingCycleDay} (crudo, se clampa a 1..28).
     * <ul>
     *   <li>{@code start} = primera fecha &lt;= today cuyo día-del-mes es {@code clampedDay}.</li>
     *   <li>{@code end}   = start + 1 mes - 1 día (inclusive).</li>
     * </ul>
     */
    public static CycleWindow windowFor(LocalDate today, int billingCycleDay) {
        int d = clampCycleDay(billingCycleDay);
        int dayOfThisMonth = Math.min(d, today.lengthOfMonth());
        LocalDate anchor = today.withDayOfMonth(dayOfThisMonth);
        LocalDate start = today.isBefore(anchor) ? anchor.minusMonths(1) : anchor;
        // Ajustar start al día clampeado (por si el mes previo era corto y anchor se ubicó en su fin).
        // Con cap en 28 esto es no-op, pero queda defensivo.
        int startDay = Math.min(d, start.lengthOfMonth());
        start = start.withDayOfMonth(startDay);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return new CycleWindow(start, end);
    }

    public record CycleWindow(LocalDate start, LocalDate end) {}
}
