package com.chatbotsaas.chatbot_saas.shared.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Utilidades de tiempo centralizadas en la zona horaria del negocio (Colombia, UTC-5).
 * <p>
 * Un único punto de verdad para {@code "hoy"} evita inconsistencias entre componentes
 * (p.ej. job de log vs. servicio de cuota) y simplifica los tests.
 */
public final class TimeUtils {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private TimeUtils() {}

    public static LocalDate todayBogota() {
        return LocalDate.now(BOGOTA);
    }
}
