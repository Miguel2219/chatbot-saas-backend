-- ============================================================================
-- V3: Sistema de cuotas mensuales por ciclo de facturación por tenant.
-- ============================================================================

-- 1) Catálogo de planes de suscripción.
CREATE TABLE IF NOT EXISTS subscription_plans (
    id                            UUID PRIMARY KEY,
    code                          VARCHAR(50)  NOT NULL UNIQUE,
    name                          VARCHAR(100) NOT NULL,
    cycle_conversations_limit     INTEGER      NOT NULL,
    excess_conversation_cost_cop  INTEGER      NOT NULL,
    max_advisers                  INTEGER,
    max_documents                 INTEGER,
    max_document_size_mb          INTEGER,
    is_active                     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed: único plan por ahora.
INSERT INTO subscription_plans (id, code, name, cycle_conversations_limit, excess_conversation_cost_cop)
VALUES (gen_random_uuid(), 'BASIC', 'Plan Básico', 2000, 100)
ON CONFLICT (code) DO NOTHING;

-- 2) Campos de ciclo en tenants + FK a plan.
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS subscription_plan_id UUID REFERENCES subscription_plans(id),
    ADD COLUMN IF NOT EXISTS billing_cycle_day    SMALLINT,
    ADD COLUMN IF NOT EXISTS current_cycle_start  DATE,
    ADD COLUMN IF NOT EXISTS current_cycle_end    DATE;

-- Backfill: computa billing_cycle_day desde created_at (zona Bogotá), cap en 28.
-- Y computa current_cycle_start / current_cycle_end espejando QuotaCycleCalculator.windowFor():
--   d       = LEAST(día-del-mes de created_at en Bogotá, 28)
--   today   = fecha actual en Bogotá
--   anchor  = today con día = LEAST(d, días-del-mes-de-today)
--   start   = today < anchor ? anchor - 1 month : anchor
--   end     = start + 1 month - 1 day
WITH today_bogota AS (
    SELECT (now() AT TIME ZONE 'America/Bogota')::date AS today
),
cycles AS (
    SELECT
        t.tenant_id,
        LEAST(EXTRACT(DAY FROM (t.created_at AT TIME ZONE 'America/Bogota'))::int, 28) AS d,
        tb.today,
        make_date(
            EXTRACT(YEAR  FROM tb.today)::int,
            EXTRACT(MONTH FROM tb.today)::int,
            LEAST(
                LEAST(EXTRACT(DAY FROM (t.created_at AT TIME ZONE 'America/Bogota'))::int, 28),
                EXTRACT(DAY FROM (date_trunc('month', tb.today) + interval '1 month' - interval '1 day'))::int
            )
        ) AS anchor
    FROM tenants t CROSS JOIN today_bogota tb
),
resolved AS (
    SELECT
        tenant_id,
        d,
        CASE WHEN today < anchor THEN (anchor - interval '1 month')::date
             ELSE anchor
        END AS cycle_start
    FROM cycles
)
UPDATE tenants t
SET subscription_plan_id = (SELECT id FROM subscription_plans WHERE code = 'BASIC'),
    billing_cycle_day    = r.d,
    current_cycle_start  = r.cycle_start,
    current_cycle_end    = (r.cycle_start + interval '1 month' - interval '1 day')::date
FROM resolved r
WHERE t.tenant_id = r.tenant_id;

ALTER TABLE tenants ALTER COLUMN subscription_plan_id SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN billing_cycle_day    SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN current_cycle_start  SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN current_cycle_end    SET NOT NULL;

-- 3) Contador por ciclo.
CREATE TABLE IF NOT EXISTS tenant_quota_cycles (
    id                         UUID PRIMARY KEY,
    tenant_id                  UUID      NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    cycle_start                DATE      NOT NULL,
    cycle_end                  DATE      NOT NULL,
    conversations_count        INTEGER   NOT NULL DEFAULT 0,
    plan_limit_snapshot        INTEGER   NOT NULL,
    excess_cost_snapshot_cop   INTEGER   NOT NULL,
    notified_80                BOOLEAN   NOT NULL DEFAULT FALSE,
    notified_100               BOOLEAN   NOT NULL DEFAULT FALSE,
    notified_150               BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenant_cycle UNIQUE (tenant_id, cycle_start)
);

CREATE INDEX IF NOT EXISTS idx_tqc_tenant    ON tenant_quota_cycles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tqc_cycle_end ON tenant_quota_cycles(cycle_end);

-- 4) Deduplicación por (tenant, ciclo, sesión, día). PK compuesta = atomicidad via INSERT ON CONFLICT DO NOTHING.
CREATE TABLE IF NOT EXISTS quota_session_days (
    tenant_id     UUID         NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    cycle_start   DATE         NOT NULL,
    session_id    VARCHAR(255) NOT NULL,
    day           DATE         NOT NULL,
    first_seen_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, cycle_start, session_id, day)
);
