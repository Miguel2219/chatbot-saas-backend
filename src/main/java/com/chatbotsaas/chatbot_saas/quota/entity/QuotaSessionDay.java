package com.chatbotsaas.chatbot_saas.quota.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Marker de "sesión vista" dentro de un ciclo de facturación y un día específico.
 * El INSERT se hace vía query nativa con {@code ON CONFLICT DO NOTHING} en el repositorio
 * — atómico, race-safe, y la PK compuesta garantiza la deduplicación.
 */
@Entity
@Table(name = "quota_session_days")
@IdClass(QuotaSessionDayId.class)
@Getter
@NoArgsConstructor
public class QuotaSessionDay {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "cycle_start", nullable = false)
    private LocalDate cycleStart;

    @Id
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;
}
