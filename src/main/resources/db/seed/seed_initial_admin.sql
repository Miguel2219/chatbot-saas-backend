-- ============================================================================
-- Initial administrator seed
-- ============================================================================
-- Creates the first administrator account so the platform can be accessed
-- after a clean deployment.
--
-- The seeded credential is single-use: the account is flagged so the
-- application forces a password change on first login, and the initial value
-- is unusable afterwards. The stored value is a BCrypt hash — the plaintext
-- is not kept in this repository.
--
-- This seed is meant to run once, on an empty database. Re-running it against
-- an existing installation is not supported.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. User (ADMIN sin tenant propio)
-- ----------------------------------------------------------------------------
--   Password : single-use, forced to change on first login
INSERT INTO users (
    user_id,
    email,
    password,
    must_change_password,
    notification_channel,
    tenant_id,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'lmangelryl1512@gmail.com',
    '$2b$10$tu.a1Q/kY.h0n2OAGPr4O.Rk.2AGqlJ9jjGSVzgDYzxp2W3MUulTC',
    TRUE,
    NULL,
    NULL,
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Person asociado (OneToOne con @MapsId — comparte user_id como PK)
-- ----------------------------------------------------------------------------
-- La entity Person tiene `@MapsId` + `@JoinColumn(name = "user_id")`, así que
-- en la tabla `person` la PK se llama `user_id` y es también la FK a users.
INSERT INTO person (
    user_id,
    name,
    lastname,
    phone,
    number_document,
    available
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Miguel',
    'Paba',
    NULL,
    NULL,
    TRUE
)
ON CONFLICT (user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Asignar ADMIN_ROLE
-- ----------------------------------------------------------------------------
-- UUID de ADMIN_ROLE hardcoded en V1__create_initial_data.sql y
-- com.chatbotsaas.chatbot_saas.role.constant.RoleConstants.ADMIN_ROLE.
INSERT INTO user_roles (user_id, role_id)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b'
)
ON CONFLICT DO NOTHING;

COMMIT;

-- ----------------------------------------------------------------------------
-- 4. Confirmación visual — debe mostrar exactamente 1 fila con role=ADMIN
-- ----------------------------------------------------------------------------
SELECT
    u.user_id,
    u.email,
    u.must_change_password,
    u.tenant_id,
    p.name,
    p.lastname,
    r.name AS role,
    u.created_at
FROM users u
    LEFT JOIN person     p  ON p.user_id  = u.user_id
    LEFT JOIN user_roles ur ON ur.user_id = u.user_id
    LEFT JOIN roles      r  ON r.role_id  = ur.role_id
WHERE u.email = 'lmangelryl1512@gmail.com';
