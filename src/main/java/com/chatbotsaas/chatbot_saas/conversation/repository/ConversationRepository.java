package com.chatbotsaas.chatbot_saas.conversation.repository;

import com.chatbotsaas.chatbot_saas.conversation.entity.Conversation;
import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    boolean existsBySessionId(String sessionId);

    Optional<Conversation> findTopByBotIdAndSessionIdOrderByCreatedAtDesc(UUID botId, String sessionId);

    /**
     * Trae los últimos 40 mensajes de la sesión (= 20 turnos de chat máximo)
     * en orden DESC. El caller debe invertir la lista para obtener orden
     * cronológico ASC, que es lo que espera el RAG en {@code chat_history}.
     *
     * <p>40 = {@code chat_history_max_turns (20) × 2} alineado con el cap
     * defensivo del RAG en {@code services/rag_service.py}. Si el límite
     * cambia en el RAG, este número debe acompañar.
     */
    List<Conversation> findTop40ByBotIdAndSessionIdOrderByCreatedAtDesc(UUID botId, String sessionId);

    @Query("SELECT c FROM Conversation c WHERE c.botId = :botId")
    Page<Conversation> findByBotId(@Param("botId") UUID botId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.botId IN (SELECT b.botId FROM Bot b WHERE b.tenant.id = :tenantId)")
    Page<Conversation> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Bulk-update del status de conversaciones que llevan demasiado tiempo
     * en {@code oldStatus} sin actividad. Usado por
     * {@code ConversationResetJob} para reactivar el bot en sesiones de
     * WhatsApp que quedaron en {@code PENDING_HUMAN} y nunca fueron
     * resueltas manualmente por un asesor.
     *
     * <p>Comparamos contra {@code createdAt} (no {@code updatedAt} —
     * la entidad no expone ese campo) porque en el modelo actual el
     * último row de una sesión tiene status sincronizado con la última
     * actividad: cuando el bot está suprimido, ningún row nuevo se
     * inserta hasta que un asesor humano resuelva la conversación. Por
     * lo tanto {@code createdAt} de la fila con {@code status=oldStatus}
     * es semánticamente la última actividad de la sesión.
     *
     * @return cantidad de filas actualizadas
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Conversation c SET c.status = :newStatus " +
           "WHERE c.status = :oldStatus AND c.createdAt < :cutoff")
    int resetStatusForStuckSince(
            @Param("oldStatus") ConversationStatus oldStatus,
            @Param("newStatus") ConversationStatus newStatus,
            @Param("cutoff") LocalDateTime cutoff);
}
