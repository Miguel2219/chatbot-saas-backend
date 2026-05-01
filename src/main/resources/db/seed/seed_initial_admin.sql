-- ============================================================================
-- Seed: ADMIN inicial de Zolvion
-- ============================================================================
--
-- Propósito
-- ---------
-- Crear el primer usuario con rol ADMIN de la plataforma. El ADMIN de Zolvion
-- no se crea automáticamente en ninguna migración Flyway a propósito: las
-- credenciales quedan fuera del código versionado y la ejecución es manual y
-- deliberada. Este archivo vive en `src/main/resources/db/seed/` (NO en
-- `db/migration/`) justamente para que Flyway no lo corra solo.
--
-- Cuándo usarlo
-- -------------
--   * Local dev: tras `DROP SCHEMA public CASCADE` + arrancar la app una vez
--     (para que Hibernate/Flyway recreen las tablas y corran V1..V5), luego
--     apagar la app y ejecutar este script.
--   * Producción: una sola vez, después del primer despliegue exitoso.
--
-- Cómo usarlo
-- -----------
--   psql -U postgres -d chatbot_saas -f src/main/resources/db/seed/seed_initial_admin.sql
--
-- O desde la raíz del repo:
--   psql -h localhost -U postgres -d chatbot_saas \
--        -f chatbot-saas-backend/src/main/resources/db/seed/seed_initial_admin.sql
--
-- El script es idempotente: si el ADMIN ya existe, los INSERTs no hacen nada
-- (ON CONFLICT DO NOTHING) y la SELECT final te muestra el estado actual.
--
-- Credenciales de primer login
-- ----------------------------
--   Email    : lmangelryl1512@gmail.com
--   Password : Zolvion2026!
--
-- `must_change_password` queda en TRUE — el ADMIN de Zolvion configura el
-- sistema, ya conoce la contraseña y la puede rotar desde el panel cuando
-- quiera. El flag TRUE aplica al flujo de invitación (users creados con
-- password temporal).
--
-- Nota sobre el hash
-- ------------------
-- El hash quemado abajo es BCrypt ($2b$10$) generado a partir de "Zolvion2026!".
-- Spring Security's BCryptPasswordEncoder.matches() acepta tanto $2a$ como $2b$
-- (ambos son el mismo algoritmo; $2b$ es la variante "moderna" post-2014).
-- Si en algún momento hay que rotar la contraseña inicial: regenerar el hash
-- con cualquier librería bcrypt estándar y reemplazar el literal de abajo.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. User (ADMIN sin tenant propio)
-- ----------------------------------------------------------------------------
-- password = BCrypt("Zolvion2026!")
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
