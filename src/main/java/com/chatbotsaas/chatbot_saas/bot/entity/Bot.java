package com.chatbotsaas.chatbot_saas.bot.entity;

import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bots")
@NoArgsConstructor
@Getter
public class Bot {

    @Id
    @Column(name = "bot_id")
    @GeneratedValue
    @UuidGenerator
    private UUID botId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Bot(String name, String description, Tenant tenant, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.tenant = tenant;
        this.isActive = isActive;
    }


    public static Bot create(String name, String description, Tenant tenant) {
        return new Bot(name, description, tenant, Boolean.TRUE);
    }

}
