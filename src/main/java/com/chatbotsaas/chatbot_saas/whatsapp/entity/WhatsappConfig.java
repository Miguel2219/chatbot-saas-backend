package com.chatbotsaas.chatbot_saas.whatsapp.entity;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_configs")
@Getter
@NoArgsConstructor
public class WhatsappConfig {

    @Id
    @Column(name = "id")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne
    @JoinColumn(name = "bot_id", nullable = false, unique = true)
    private Bot bot;

    @Column(name = "phone_number_id", nullable = false)
    private String phoneNumberId;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public WhatsappConfig(Bot bot, String phoneNumberId, String apiKey, Boolean isActive) {
        this.bot = bot;
        this.phoneNumberId = phoneNumberId;
        this.apiKey = apiKey;
        this.isActive = isActive;
    }

    public static WhatsappConfig create(Bot bot, String phoneNumberId, String apiKey) {
        return new WhatsappConfig(bot, phoneNumberId, apiKey, Boolean.TRUE);
    }

    public void update(String phoneNumberId, String apiKey) {
        this.phoneNumberId = phoneNumberId;
        this.apiKey = apiKey;
    }
}
