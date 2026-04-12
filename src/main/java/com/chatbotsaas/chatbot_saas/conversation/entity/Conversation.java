package com.chatbotsaas.chatbot_saas.conversation.entity;

import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    @Id
    @Column(name = "conversation_id")
    @UuidGenerator
    @GeneratedValue
    private UUID id;

    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false)
    private ConversationStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {this.createdAt = LocalDateTime.now();}

    public Conversation(UUID botId, String sessionId, String role, String message, ConversationStatus status) {
        this.botId = botId;
        this.sessionId = sessionId;
        this.role = role;
        this.message = message;
        this.status = status;
    }

    public static Conversation create(UUID botId, String sessionId, String role, String message) {
        return new Conversation(botId, sessionId, role, message, ConversationStatus.BOT_ACTIVE);
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }
}
