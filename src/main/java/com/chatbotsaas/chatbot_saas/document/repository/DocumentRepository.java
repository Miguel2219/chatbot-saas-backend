package com.chatbotsaas.chatbot_saas.document.repository;

import com.chatbotsaas.chatbot_saas.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByBotId(UUID botId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.botId IN (SELECT b.botId FROM Bot b WHERE b.tenant.id = :tenantId)")
    Page<Document> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
}
