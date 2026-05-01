-- ============================================================================
-- V7: Tabla refresh_tokens — soporte de refresh-token rotation con revocación.
-- ============================================================================
--
-- Motivación
-- ----------
-- Antes del plan de refresh tokens, la auth emitía un solo JWT de 24h. Eso
-- hacía imposible: (1) cerrar sesión de forma segura (no se puede invalidar
-- un JWT sin blacklist), (2) detectar robo de tokens, (3) rotar credenciales
-- sin obligar a re-loguear. Cambiamos a un esquema estándar:
--   - Access token: JWT firmado, corto (1h), stateless. NO toca DB.
--   - Refresh token: string opaco de 32 bytes aleatorios, largo (7d),
--     PERSISTIDO AQUÍ. Cada uso genera uno nuevo y revoca el anterior
--     (rotación). Si llega uno ya revocado, revocamos todos los del user
--     como señal de reuso (posible robo).
--
-- ¿Por qué SHA-256 y no BCrypt para el hash?
-- ------------------------------------------
-- BCrypt es no-determinístico (cada encode() produce un hash distinto),
-- así que no se puede hacer lookup-by-hash con un índice UNIQUE. Tendrías
-- que traer todos los RT no revocados del user y probar matches() uno por
-- uno — O(n) en cada /refresh. SHA-256 es determinístico, indexable, y el
-- input ya es 256 bits aleatorios (no es una password adivinable), así que
-- el argumento "BCrypt es más seguro" no aplica acá.
--
-- FK ON DELETE CASCADE
-- --------------------
-- Si se borra el user, los RTs se van solos — la tabla no tiene valor
-- histórico propio, son material operativo de sesiones vivas.
-- ============================================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
