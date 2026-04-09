package com.chatbotsaas.chatbot_saas.conversation.repository;

import com.chatbotsaas.chatbot_saas.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByBotIdOrderByCreatedAtAsc(UUID botId);

    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    boolean existsBySessionId(String sessionId);

    Optional<Conversation> findTopByBotIdAndSessionIdOrderByCreatedAtDesc(UUID botId, String sessionId);
}
