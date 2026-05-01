-- V10 — Rename de la columna `api_key` → `access_token` en
-- whatsapp_configs.
--
-- Contexto: la migración del proveedor WhatsApp (de 360dialog a Meta
-- Cloud API) cambió la semántica del valor almacenado. Antes era una
-- API key estática de 360dialog; ahora es un Permanent Access Token
-- de Meta usado como `Authorization: Bearer <token>`. El campo se
-- renombra para reflejar la semántica nueva.
--
-- Nota: la tabla `whatsapp_configs` originalmente fue creada por
-- Hibernate DDL (la migración V2__add_whatsapp_config.sql está vacía
-- a propósito). Esta migration es la primera modificación versionada
-- de schema sobre esa tabla.

ALTER TABLE whatsapp_configs
    RENAME COLUMN api_key TO access_token;
