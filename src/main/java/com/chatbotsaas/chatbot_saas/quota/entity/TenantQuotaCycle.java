package com.chatbotsaas.chatbot_saas.quota.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_quota_cycles",
        uniqueConstraints = @UniqueConstraint(name = "uq_tenant_cycle", columnNames = {"tenant_id", "cycle_start"})
)
@Getter
@NoArgsConstructor
public class TenantQuotaCycle {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "cycle_start", nullable = false)
    private LocalDate cycleStart;

    @Column(name = "cycle_end", nullable = false)
    private LocalDate cycleEnd;

    @Column(name = "conversations_count", nullable = false)
    private Integer conversationsCount;

    @Column(name = "plan_limit_snapshot", nullable = false)
    private Integer planLimitSnapshot;

    @Column(name = "excess_cost_snapshot_cop", nullable = false)
    private Integer excessCostSnapshotCop;

    @Column(name = "notified_80", nullable = false)
    private Boolean notified80;

    @Column(name = "notified_100", nullable = false)
    private Boolean notified100;

    @Column(name = "notified_150", nullable = false)
    private Boolean notified150;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private TenantQuotaCycle(
            UUID tenantId,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            Integer planLimitSnapshot,
            Integer excessCostSnapshotCop
    ) {
        this.tenantId = tenantId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.planLimitSnapshot = planLimitSnapshot;
        this.excessCostSnapshotCop = excessCostSnapshotCop;
        this.conversationsCount = 0;
        this.notified80 = Boolean.FALSE;
        this.notified100 = Boolean.FALSE;
        this.notified150 = Boolean.FALSE;
    }

    public static TenantQuotaCycle create(
            UUID tenantId,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            Integer planLimitSnapshot,
            Integer excessCostSnapshotCop
    ) {
        return new TenantQuotaCycle(tenantId, cycleStart, cycleEnd, planLimitSnapshot, excessCostSnapshotCop);
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.conversationsCount == null) this.conversationsCount = 0;
        if (this.notified80 == null) this.notified80 = Boolean.FALSE;
        if (this.notified100 == null) this.notified100 = Boolean.FALSE;
        if (this.notified150 == null) this.notified150 = Boolean.FALSE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
