package com.chatbotsaas.chatbot_saas.quota.repository;

import com.chatbotsaas.chatbot_saas.quota.entity.TenantQuotaCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantQuotaCycleRepository extends JpaRepository<TenantQuotaCycle, UUID> {

    Optional<TenantQuotaCycle> findByTenantIdAndCycleStart(UUID tenantId, LocalDate cycleStart);

    List<TenantQuotaCycle> findByTenantIdOrderByCycleStartDesc(UUID tenantId);

    /**
     * Incrementa atómicamente el contador del ciclo. Retorna el número de filas afectadas.
     * <p>
     * Nota: antes usábamos {@code UPDATE ... RETURNING conversations_count} en un solo round-trip,
     * pero Spring Data JPA + {@code @Modifying} no soporta queries que devuelven datos
     * ("Se retornó un resultado cuando no se esperaba ninguno"). Por eso separamos en
     * increment + {@link #findConversationsCountById(UUID)}. El UPDATE sigue siendo atómico
     * en PostgreSQL; la diferencia es 2 round-trips en vez de 1.
     */
    @Modifying
    @Query(
            value = "UPDATE tenant_quota_cycles " +
                    "SET conversations_count = conversations_count + 1, " +
                    "    updated_at = NOW() " +
                    "WHERE id = :id",
            nativeQuery = true
    )
    int increment(@Param("id") UUID id);

    /**
     * Lee el contador actual de conversaciones. Se usa después de {@link #increment(UUID)}
     * para obtener el valor post-update, ya que {@code @Modifying} no puede retornar datos.
     */
    @Query(
            value = "SELECT conversations_count FROM tenant_quota_cycles WHERE id = :id",
            nativeQuery = true
    )
    Integer findConversationsCountById(@Param("id") UUID id);

    /**
     * Marca atómicamente el flag de 80% si aún estaba en false. Retorna 1 si ganó la carrera; 0 si otro ya lo marcó.
     */
    @Modifying
    @Query(
            value = "UPDATE tenant_quota_cycles " +
                    "SET notified_80 = TRUE, updated_at = NOW() " +
                    "WHERE id = :id AND notified_80 = FALSE",
            nativeQuery = true
    )
    int markNotified80(@Param("id") UUID id);

    @Modifying
    @Query(
            value = "UPDATE tenant_quota_cycles " +
                    "SET notified_100 = TRUE, updated_at = NOW() " +
                    "WHERE id = :id AND notified_100 = FALSE",
            nativeQuery = true
    )
    int markNotified100(@Param("id") UUID id);

    @Modifying
    @Query(
            value = "UPDATE tenant_quota_cycles " +
                    "SET notified_150 = TRUE, updated_at = NOW() " +
                    "WHERE id = :id AND notified_150 = FALSE",
            nativeQuery = true
    )
    int markNotified150(@Param("id") UUID id);
}
