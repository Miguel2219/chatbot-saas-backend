package com.chatbotsaas.chatbot_saas.quota.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Getter
@NoArgsConstructor
public class SubscriptionPlan {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "cycle_conversations_limit", nullable = false)
    private Integer cycleConversationsLimit;

    @Column(name = "excess_conversation_cost_cop", nullable = false)
    private Integer excessConversationCostCop;

    @Column(name = "max_advisers")
    private Integer maxAdvisers;

    @Column(name = "max_documents")
    private Integer maxDocuments;

    @Column(name = "max_document_size_mb")
    private Integer maxDocumentSizeMb;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.isActive == null) this.isActive = Boolean.TRUE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
