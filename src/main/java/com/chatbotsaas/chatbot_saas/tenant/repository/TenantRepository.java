package com.chatbotsaas.chatbot_saas.tenant.repository;

import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByEmail(String email);

    /**
     * Tenants cuyo ciclo actual venció antes de la fecha indicada (i.e., ya están en un ciclo nuevo).
     * Usado por el {@code QuotaCycleLogJob} para observabilidad diaria.
     */
    List<Tenant> findByCurrentCycleEndBefore(LocalDate date);
}
