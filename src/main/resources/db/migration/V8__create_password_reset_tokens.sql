-- ============================================================================
-- V8: Tabla password_reset_tokens — soporte del flujo "forgot password".
-- ============================================================================
--
-- Motivación
-- ----------
-- El único camino hoy para cambiar la contraseña es /auth/change_password,
-- que exige sesión activa. Si un user olvida su password, queda fuera sin
-- camino de recuperación propio — dependía del admin. Este flujo permite:
--   1. POST /auth/forgot-password con el email → se emite un token y le
--      llega un link por correo (expira en 1h por defecto).
--   2. GET /auth/reset-password/validate?token=… valida y devuelve el email
--      enmascarado para mostrar al user.
--   3. POST /auth/reset-password con token + new_password → cambia la pass,
--      marca el token como used=true y REVOCA todos los refresh tokens del
--      user (cierre de sesiones activas en cualquier otro device).
--
-- Por qué SHA-256 y no BCrypt (igual que refresh_tokens)
-- ------------------------------------------------------
-- BCrypt es no-determinístico → no se puede lookup-by-hash con índice UNIQUE.
-- El token raw son 256 bits aleatorios, no una password adivinable: SHA-256
-- es suficiente y permite índice único para O(log n) en la validación.
--
-- FK ON DELETE CASCADE
-- --------------------
-- Los tokens son material operativo sin valor histórico: si el user se borra
-- se van con él.
--
-- Índice (user_id, created_at)
-- ----------------------------
-- Alimenta el rate-limit: "¿cuántos tokens emití a este user en los últimos
-- 15 min?". La query del repo es
--   count(*) from password_reset_tokens where user_id=? and created_at > ?
-- → este índice compuesto la resuelve sin table scan.
-- ============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_prt_user_id_created ON password_reset_tokens(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_prt_expires_at      ON password_reset_tokens(expires_at);
