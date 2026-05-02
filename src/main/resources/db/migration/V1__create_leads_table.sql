-- ============================================================================
-- V1 — Crear tabla `leads`
-- ============================================================================
-- Contexto: la entity Lead.java existió desde el inicio, pero la tabla
-- la creaba Hibernate con `ddl-auto=update`. Cuando se cambió a
-- `ddl-auto=validate` + Flyway, nunca se versionó el CREATE TABLE leads.
-- Esta migración suple ese gap.
--
-- Mapeo desde Lead.java:
--   leadId             → lead_id              UUID, PK
--   botId              → bot_id               UUID, NOT NULL
--   sessionId          → session_id           VARCHAR, NOT NULL
--   name               → name                 VARCHAR, NOT NULL
--   phone              → phone                VARCHAR, NULL
--   email              → email                VARCHAR, NULL
--   requestDetail      → request_detail       TEXT, NULL
--   leadChannel        → channel              VARCHAR (enum STRING), NOT NULL
--   assignedAdviserId  → assigned_adviser_id  UUID, NULL
--   status             → status               VARCHAR (enum STRING), NULL
--   createdAt          → created_at           TIMESTAMP, NULL (set en @PrePersist)
-- ============================================================================

CREATE TABLE IF NOT EXISTS leads (
    lead_id              UUID NOT NULL,
    bot_id               UUID NOT NULL,
    session_id           VARCHAR(255) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    phone                VARCHAR(255),
    email                VARCHAR(255),
    request_detail       TEXT,
    channel              VARCHAR(50) NOT NULL,
    assigned_adviser_id  UUID,
    status               VARCHAR(50),
    created_at           TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT leads_pkey PRIMARY KEY (lead_id)
);
