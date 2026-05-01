package com.chatbotsaas.chatbot_saas.quota.repository;

import com.chatbotsaas.chatbot_saas.quota.entity.QuotaSessionDay;
import com.chatbotsaas.chatbot_saas.quota.entity.QuotaSessionDayId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface QuotaSessionDayRepository extends JpaRepository<QuotaSessionDay, QuotaSessionDayId> {

    /**
     * Inserta (tenant, ciclo, sesión, día) si aún no existe. Atómico gracias a la PK compuesta + ON CONFLICT.
     * Retorna 1 si la fila fue efectivamente creada (primera vez que se ve esa sesión ese día para ese tenant
     * y ciclo); 0 si ya existía (no se debe incrementar el contador).
     */
    @Modifying
    @Query(
            value = "INSERT INTO quota_session_days (tenant_id, cycle_start, session_id, day, first_seen_at) " +
                    "VALUES (:tenantId, :cycleStart, :sessionId, :day, NOW()) " +
                    "ON CONFLICT DO NOTHING",
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("tenantId") UUID tenantId,
            @Param("cycleStart") LocalDate cycleStart,
            @Param("sessionId") String sessionId,
            @Param("day") LocalDate day
    );

    @Query(
            value = "SELECT COUNT(*) FROM quota_session_days " +
                    "WHERE tenant_id = :tenantId AND cycle_start = :cycleStart",
            nativeQuery = true
    )
    long countByTenantIdAndCycleStart(
            @Param("tenantId") UUID tenantId,
            @Param("cycleStart") LocalDate cycleStart
    );

    @Query(
            value = "SELECT COUNT(*) FROM quota_session_days " +
                    "WHERE tenant_id = :tenantId AND cycle_start = :cycleStart AND session_id = :sessionId",
            nativeQuery = true
    )
    long countByTenantIdAndCycleStartAndSessionId(
            @Param("tenantId") UUID tenantId,
            @Param("cycleStart") LocalDate cycleStart,
            @Param("sessionId") String sessionId
    );

    /**
     * Cuenta conversaciones (sesión-día únicas) del tenant en un día calendario,
     * independientemente del ciclo. Usado por el Dashboard para "Conversaciones hoy".
     * <p>
     * Se deja sin filtro de {@code cycle_start} a propósito: si un día calendario se
     * solapa con el cambio de ciclo (caso borde del día del corte), las sesiones de ambas
     * mitades cuentan como conversaciones del día. El Dashboard muestra "hoy" como día
     * calendario de Bogotá — ese es el contrato del campo.
     */
    long countByTenantIdAndDay(UUID tenantId, LocalDate day);
}
