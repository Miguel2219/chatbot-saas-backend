-- ============================================================================
-- V5 — Refactor de roles: eliminar ADVISER + introducir TENANT_OWNER
-- ============================================================================
-- Contexto:
--   - ADVISER queda obsoleto. WhatsApp hace handoff a humano en 360dialog
--     (fuera de Zolvion); Widget hace fan-out entre users del tenant.
--   - TENANT_OWNER es el primer user de cada tenant; único autorizado a
--     invitar otros users (siempre como USER) y a ser el "dueño" del tenant.
--   - bot_advisers NO se borra — LeadService.saveLead() la usa en producción
--     para round-robin. Se renombra a bot_lead_assignees, conceptualmente
--     cualquier user del tenant puede ser lead-assignee.
-- ============================================================================

-- ─── 1.1 Eliminar rol ADVISER ───────────────────────────────────────────────
-- Orden importante: user_roles y role_permissions antes que roles (FK).
DELETE FROM user_roles
WHERE role_id = '4c7a9e25-1b3f-4d8c-a762-0e5b8f2d4c9a';

DELETE FROM role_permissions
WHERE role_id = '4c7a9e25-1b3f-4d8c-a762-0e5b8f2d4c9a';

DELETE FROM roles
WHERE role_id = '4c7a9e25-1b3f-4d8c-a762-0e5b8f2d4c9a';


-- ─── 1.2 Crear rol TENANT_OWNER ─────────────────────────────────────────────
INSERT INTO roles (role_id, name, description, created_at)
VALUES (
    '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48',
    'TENANT_OWNER',
    'Dueño del tenant — administra su tenant e invita usuarios',
    NOW()
)
ON CONFLICT (role_id) DO NOTHING;


-- ─── 1.3 Agregar permission EDIT para módulo 'tenants' + asignarlo a ADMIN ──
-- V1 dejó el módulo 'tenants' con sólo VIEW/CREATE/DELETE. Para el nuevo
-- PUT /api/tenants/{id} hace falta la acción EDIT.
INSERT INTO permissions (permission_id, module_id, action)
VALUES (
    'b4a1c8e7-2d5f-4a6e-9b3c-8f1d7e0c9a3b',
    '4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a',
    'EDIT'
)
ON CONFLICT (permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
VALUES (
    'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b',
    'b4a1c8e7-2d5f-4a6e-9b3c-8f1d7e0c9a3b'
)
ON CONFLICT DO NOTHING;


-- ─── 1.4 Asignar permisos operativos a TENANT_OWNER ─────────────────────────
-- Todos los permisos sobre los módulos operativos del tenant. NO incluye:
--   - tenants, roles, quotas  → exclusivos de ADMIN Zolvion
INSERT INTO role_permissions (role_id, permission_id)
SELECT '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48', p.permission_id
FROM permissions p
JOIN modules m ON m.module_id = p.module_id
WHERE m.route IN (
    'dashboard',
    'bots',
    'leads',
    'conversations',
    'documents',
    'users',
    'whatsapp-config'
)
ON CONFLICT DO NOTHING;


-- ─── 1.5 Promover user más antiguo de cada tenant → TENANT_OWNER ────────────
-- Regla: el user más antiguo de cada tenant pierde USER_ROLE (queda
-- implícito en TENANT_OWNER) y gana TENANT_OWNER_ROLE. Otros roles custom
-- que pudiera tener el user se preservan — no tocamos nada fuera de estas
-- dos entradas específicas.
DO $$
DECLARE
    t_row       RECORD;
    oldest_user UUID;
BEGIN
    FOR t_row IN SELECT tenant_id FROM tenants LOOP
        SELECT user_id INTO oldest_user
        FROM users
        WHERE tenant_id = t_row.tenant_id
        ORDER BY created_at ASC
        LIMIT 1;

        IF oldest_user IS NOT NULL THEN
            -- Quitar USER (sustituido conceptualmente por TENANT_OWNER).
            DELETE FROM user_roles
            WHERE user_id = oldest_user
              AND role_id = '9b1d5f73-2a8e-4c6d-b394-7e0f2a5c8d1e';

            -- Agregar TENANT_OWNER (idempotente).
            INSERT INTO user_roles (user_id, role_id)
            VALUES (oldest_user, '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48')
            ON CONFLICT DO NOTHING;
        END IF;
    END LOOP;
END $$;


-- ─── 1.6 Renombrar bot_advisers → bot_lead_assignees + columna índice ──────
-- El @JoinTable de Bot.java usaba name = "bot_advisers". Hibernate gestiona
-- la tabla sin DDL explícito en V1, pero existe en la DB. El rename preserva
-- todos los datos de round-robin actuales.
ALTER TABLE IF EXISTS bot_advisers RENAME TO bot_lead_assignees;
ALTER TABLE bots      RENAME COLUMN last_adviser_index TO last_assignee_index;


-- ─── 1.7 Validaciones post-migración ────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM user_roles
        WHERE role_id = '4c7a9e25-1b3f-4d8c-a762-0e5b8f2d4c9a'
    ) THEN
        RAISE EXCEPTION 'V5: quedan user_roles apuntando al ADVISER_ROLE eliminado';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM roles WHERE role_id = '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48'
    ) THEN
        RAISE EXCEPTION 'V5: TENANT_OWNER no se creó';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM permissions p
        JOIN modules m ON m.module_id = p.module_id
        WHERE m.route = 'tenants' AND p.action = 'EDIT'
    ) THEN
        RAISE EXCEPTION 'V5: permission tenants:EDIT no se creó';
    END IF;

    -- ADMIN tiene que tener asignado el nuevo permission.
    IF NOT EXISTS (
        SELECT 1 FROM role_permissions
        WHERE role_id      = 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b'
          AND permission_id = 'b4a1c8e7-2d5f-4a6e-9b3c-8f1d7e0c9a3b'
    ) THEN
        RAISE EXCEPTION 'V5: ADMIN no tiene asignado tenants:EDIT';
    END IF;
END $$;
