package com.chatbotsaas.chatbot_saas.lead.entity;

import com.chatbotsaas.chatbot_saas.lead.enums.LeadChannel;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lead {

    @Id
    @Column(name = "lead_id")
    @GeneratedValue
    @UuidGenerator
    private UUID leadId;

    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = true)
    private String phone;

    @Column(name = "email", nullable = true)
    private String email;

    @Column(name = "request_detail", nullable = true, columnDefinition = "TEXT")
    private String requestDetail;

    @Column(name = "channel", nullable = false)
    private LeadChannel leadChannel;

    @Column(name = "assigned_adviser_id", nullable = true)
    private UUID assignedAdviserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LeadStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
