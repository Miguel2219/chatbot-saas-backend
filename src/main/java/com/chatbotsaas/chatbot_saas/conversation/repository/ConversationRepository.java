package com.chatbotsaas.chatbot_saas.conversation.repository;

import com.chatbotsaas.chatbot_saas.conversation.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Page<Conversation> findByBotIdOrderByCreatedAtAsc(UUID botId, Pageable pageable);

    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    boolean existsBySessionId(String sessionId);

    Optional<Conversation> findTopByBotIdAndSessionIdOrderByCreatedAtDesc(UUID botId, String sessionId);

    // Conversaciones únicas hoy (session_id + fecha)
    @Query(value = "SELECT COUNT(*) FROM (" +
            "SELECT DISTINCT session_id, DATE(created_at) " +
            "FROM conversations " +
            "WHERE DATE(created_at) = CURRENT_DATE" +
            ") AS dc",
            nativeQuery = true)
    Long countUniqueConversationsToday();

    // Conversaciones únicas este mes
    @Query(value = "SELECT COUNT(*) FROM (" +
            "SELECT DISTINCT session_id, DATE(created_at) " +
            "FROM conversations " +
            "WHERE EXTRACT(MONTH FROM created_at) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM CURRENT_DATE)" +
            ") AS dc",
            nativeQuery = true)
    Long countUniqueConversationsThisMonth();

    // Últimas 5 sesiones únicas recientes
    @Query(value = "SELECT DISTINCT ON (session_id) session_id, bot_id, created_at " +
            "FROM conversations " +
            "ORDER BY session_id, created_at DESC " +
            "LIMIT 5",
            nativeQuery = true)
    List<Object[]> findRecentUniqueSessions();



}
