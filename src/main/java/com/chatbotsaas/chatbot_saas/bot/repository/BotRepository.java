package com.chatbotsaas.chatbot_saas.bot.repository;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BotRepository extends JpaRepository<Bot, UUID> {
    List<Bot> findByTenant_Id(UUID tenantId);

    List<Bot> findByTenant_IdAndBotIdIn(UUID tenantId, Collection<UUID> botIds);

    void deleteById(UUID botId);
}
