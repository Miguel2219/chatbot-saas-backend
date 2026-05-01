-- ============================================================================
-- V6: Fija el UUID del plan BASIC en un valor determinístico.
-- ============================================================================
--
-- Motivación
-- ----------
-- V3 insertó la fila del plan BASIC con `gen_random_uuid()`, por lo que cada
-- DB recibió un UUID distinto. TenantService.createTenant necesita referenciar
-- ese plan desde código Java sin depender del string 'BASIC' (que podría
-- renombrarse), así que fijamos el UUID a un valor conocido —
-- `c1a51a01-b001-4000-a000-000000000001` — y lo exponemos como constante en
-- SubscriptionPlanConstants.BASIC_PLAN.
--
-- Esta migración es idempotente y maneja los 3 casos posibles:
--   1. La fila BASIC ya tiene el UUID objetivo  → no-op.
--   2. La fila BASIC existe con otro UUID (caso DBs que ya corrieron V3):
--      insertar fila nueva con el UUID fijo, re-apuntar tenants, borrar vieja.
--   3. La fila BASIC no existe todavía (caso primer despliegue en prod o
--      DB limpia): la inserta con el UUID fijo.
--
-- Estrategia para el caso 2: el FK tenants.subscription_plan_id apunta a
-- subscription_plans.id, así que no podemos hacer UPDATE de la PK ni mover
-- los tenants antes de que exista la fila destino. El orden correcto es:
--   a. Renombrar el `code` del registro viejo (para liberar el UNIQUE).
--   b. Insertar fila nueva con id=target_id y code='BASIC'.
--   c. Re-apuntar tenants al UUID nuevo.
--   d. Borrar la fila vieja.
--
-- NO editamos V3 en su lugar porque eso rompería el checksum de Flyway en
-- cualquier DB que ya la haya aplicado.
-- ============================================================================

DO $$
DECLARE
    target_id CONSTANT UUID := 'c1a51a01-b001-4000-a000-000000000001';
    current_id UUID;
BEGIN
    SELECT id INTO current_id FROM subscription_plans WHERE code = 'BASIC';

    IF current_id IS NULL THEN
        INSERT INTO subscription_plans (id, code, name, cycle_conversations_limit, excess_conversation_cost_cop)
        VALUES (target_id, 'BASIC', 'Plan Básico', 2000, 100);
    ELSIF current_id <> target_id THEN
        -- (a) Libera el valor único de code antes de insertar la fila nueva.
        UPDATE subscription_plans
           SET code = 'BASIC_OLD_' || current_id::text
         WHERE id = current_id;

        -- (b) Inserta la fila nueva copiando el resto de columnas de la vieja.
        INSERT INTO subscription_plans (id, code, name, cycle_conversations_limit, excess_conversation_cost_cop)
        SELECT target_id, 'BASIC', name, cycle_conversations_limit, excess_conversation_cost_cop
          FROM subscription_plans
         WHERE id = current_id;

        -- (c) Re-apunta todos los tenants al UUID nuevo.
        UPDATE tenants
           SET subscription_plan_id = target_id
         WHERE subscription_plan_id = current_id;

        -- (d) Borra la fila vieja (ya sin tenants apuntándole).
        DELETE FROM subscription_plans
         WHERE id = current_id;
    END IF;
END $$;
