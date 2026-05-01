package com.chatbotsaas.chatbot_saas.bot.repository;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BotRepository extends JpaRepository<Bot, UUID> {

    @EntityGraph(attributePaths = "tenant")
    Page<Bot> findByTenant_Id(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = "tenant")
    @Override
    Page<Bot> findAll(Pageable pageable);

    List<Bot> findByTenant_Id(UUID tenantId);

    List<Bot> findByTenant_IdAndBotIdIn(UUID tenantId, Collection<UUID> botIds);

    List<Bot> findByLeadAssigneesContaining(User user);

    void deleteById(UUID botId);
}
