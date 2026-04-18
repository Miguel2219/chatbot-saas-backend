package com.chatbotsaas.chatbot_saas.lead.repository;

import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    List<Lead> findByBotId(UUID botId);

    List<Lead> findByBotIdAndStatus(UUID botId, String status);

    List<Lead> findByAssignedAdviserId(String assignedAdviserId);

    // Total leads
    Long countBy();


}
