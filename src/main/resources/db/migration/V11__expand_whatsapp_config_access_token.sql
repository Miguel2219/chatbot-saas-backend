-- V11 — Expandir whatsapp_configs.access_token de VARCHAR(255) a TEXT.
--
-- Contexto: Hibernate creó la columna como VARCHAR(255) por defecto.
-- Los Permanent Access Tokens de Meta (System User Tokens) superan
-- esa longitud (típicamente 200-260+ caracteres). Cambiar a TEXT
-- elimina el límite arbitrario sin penalty de performance — en
-- PostgreSQL TEXT y VARCHAR(N) usan el mismo storage interno.

ALTER TABLE whatsapp_configs
    ALTER COLUMN access_token TYPE TEXT;
