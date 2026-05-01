-- ============================================================================
-- V4: Módulo de Cuotas — separado del módulo Dashboard.
-- ============================================================================
-- Razón: "Dashboard" y "Cuotas" son pantallas distintas con audiencias distintas.
-- Dashboard = resumen operativo del tenant (leads, conversaciones del día/mes);
-- Cuotas    = consumo de plan + ciclos de facturación, sólo interesa al ADMIN
--             de Zolvion para supervisar/facturar. USER y ADVISER no lo ven.
--
-- Idempotente: usa ON CONFLICT en cada INSERT (el schema tiene UNIQUE en
-- modules.name, PK en permissions.permission_id, PK compuesta en role_permissions).
-- ============================================================================

-- 1) Nuevo módulo "quotas".
--    route = 'quotas' (lo que usa @PreAuthorize("@permissionChecker.hasPermission('quotas','view')"))
--    icon  = 'bar-chart'
--    display_order = 10 (después de Roles = 9; al final del menú)
INSERT INTO modules (module_id, name, route, icon, display_order)
VALUES (
    'd1e5f2a4-8c9b-4a3d-b7e6-0f1c2d4e5a6b',
    'Cuotas',
    'quotas',
    'bar-chart',
    10
)
ON CONFLICT (name) DO NOTHING;

-- 2) Único permiso del módulo: VIEW.
--    Read-only por diseño: las cuotas se generan automáticamente con el uso,
--    no se editan ni borran manualmente. Si en el futuro aparece un caso de uso
--    (reset manual, ajuste de snapshot), se agrega CREATE/EDIT/DELETE en migración posterior.
INSERT INTO permissions (permission_id, module_id, action)
VALUES (
    'a2b3c4d5-6e7f-4a89-90ab-cdef12345678',
    'd1e5f2a4-8c9b-4a3d-b7e6-0f1c2d4e5a6b',
    'VIEW'
)
ON CONFLICT (permission_id) DO NOTHING;

-- 3) Asignar quotas:VIEW SOLO al rol ADMIN.
--    ADMIN_ROLE UUID viene de V1 y de RoleConstants.java: a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b.
--    NO se asigna a USER_ROLE ni ADVISER_ROLE — decisión de negocio.
INSERT INTO role_permissions (role_id, permission_id)
VALUES (
    'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b',
    'a2b3c4d5-6e7f-4a89-90ab-cdef12345678'
)
ON CONFLICT DO NOTHING;
