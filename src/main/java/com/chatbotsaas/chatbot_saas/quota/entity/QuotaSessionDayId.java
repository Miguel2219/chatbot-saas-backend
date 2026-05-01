package com.chatbotsaas.chatbot_saas.quota.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link QuotaSessionDay}: (tenant_id, cycle_start, session_id, day).
 */
public class QuotaSessionDayId implements Serializable {

    private UUID tenantId;
    private LocalDate cycleStart;
    private String sessionId;
    private LocalDate day;

    public QuotaSessionDayId() {}

    public QuotaSessionDayId(UUID tenantId, LocalDate cycleStart, String sessionId, LocalDate day) {
        this.tenantId = tenantId;
        this.cycleStart = cycleStart;
        this.sessionId = sessionId;
        this.day = day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuotaSessionDayId that)) return false;
        return Objects.equals(tenantId, that.tenantId)
                && Objects.equals(cycleStart, that.cycleStart)
                && Objects.equals(sessionId, that.sessionId)
                && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, cycleStart, sessionId, day);
    }
}
