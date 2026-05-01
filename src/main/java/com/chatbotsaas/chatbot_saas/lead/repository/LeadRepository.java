package com.chatbotsaas.chatbot_saas.lead.repository;

import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    List<Lead> findByBotId(UUID botId);

    List<Lead> findByBotIdAndStatus(UUID botId, String status);

    List<Lead> findByAssignedAdviserId(String assignedAdviserId);

    // Total leads
    Long countBy();

    @Query("SELECT l FROM Lead l WHERE l.botId = :botId")
    Page<Lead> findByBotId(@Param("botId") UUID botId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.botId IN (SELECT b.botId FROM Bot b WHERE b.tenant.id = :tenantId)")
    Page<Lead> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Cuenta todos los leads del tenant. Como {@code Lead} referencia al bot por UUID plano
     * (no hay {@code tenantId} directo), pasamos por una subquery sobre {@code Bot} —
     * mismo patrón que {@link #findByTenantId(UUID, Pageable)}.
     */
    @Query("SELECT COUNT(l) FROM Lead l " +
           "WHERE l.botId IN (SELECT b.botId FROM Bot b WHERE b.tenant.id = :tenantId)")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Cuenta leads del tenant filtrados por estado (p.ej. PENDING).
     * Mismo patrón que {@link #countByTenantId(UUID)}.
     */
    @Query("SELECT COUNT(l) FROM Lead l " +
           "WHERE l.status = :status " +
           "AND l.botId IN (SELECT b.botId FROM Bot b WHERE b.tenant.id = :tenantId)")
    long countByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") LeadStatus status
    );
}
