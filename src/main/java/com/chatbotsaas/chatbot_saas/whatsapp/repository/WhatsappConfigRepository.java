package com.chatbotsaas.chatbot_saas.whatsapp.repository;

import com.chatbotsaas.chatbot_saas.whatsapp.entity.WhatsappConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsappConfigRepository extends JpaRepository<WhatsappConfig, UUID> {

    Optional<WhatsappConfig> findByPhoneNumberId(String phoneNumberId);

    Optional<WhatsappConfig> findByBot_BotId(UUID botId);
}
