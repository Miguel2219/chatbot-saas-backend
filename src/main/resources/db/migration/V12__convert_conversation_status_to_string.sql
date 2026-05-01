-- V12 — Convertir conversations.status al nombre del enum como string.
--
-- Contexto: la entidad Conversation pasó a @Enumerated(EnumType.STRING).
-- En la BD la columna originalmente tenía un check constraint generado
-- por Hibernate (típicamente `conversations_status_check`) que valida
-- los valores del enum contra los ordinales (0, 1, 2). Al hacer
-- ALTER COLUMN ... TYPE VARCHAR, Postgres intenta re-validar el check
-- contra el nuevo tipo y falla con "character varying >= integer".
--
-- Solución:
--  1. Dropear el check constraint si existe (IF EXISTS para idempotencia).
--  2. Convertir el tipo con CASE sobre status::text — agnóstico a si
--     la columna era smallint, integer o varchar de strings numéricos.
--  3. Hibernate, al arrancar con la entidad nueva (@Enumerated(STRING)),
--     genera un check constraint nuevo apropiado para los valores string.
--
-- Mapping (orden declarado en ConversationStatus):
--   '0' → 'BOT_ACTIVE'
--   '1' → 'PENDING_HUMAN'
--   '2' → 'HUMAN_ACTIVE'

ALTER TABLE conversations DROP CONSTRAINT IF EXISTS conversations_status_check;

ALTER TABLE conversations
    ALTER COLUMN status TYPE VARCHAR(32)
    USING (CASE status::text
        WHEN '0' THEN 'BOT_ACTIVE'
        WHEN '1' THEN 'PENDING_HUMAN'
        WHEN '2' THEN 'HUMAN_ACTIVE'
        ELSE status::text
    END);
