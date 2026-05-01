-- Backfill: NotificationChannel.BOTH fue removido del enum Java.
-- Cualquier user con 'BOTH' en DB hace que Hibernate tire
-- IllegalArgumentException al cargarlo (ej. en /login, listados de users).
-- Mapeamos a EMAIL por ser el canal más universal — el user siempre tiene
-- mail y puede cambiarlo luego desde el panel. WhatsApp requiere teléfono
-- configurado en person, no siempre disponible.
UPDATE users
SET notification_channel = 'EMAIL'
WHERE notification_channel = 'BOTH';