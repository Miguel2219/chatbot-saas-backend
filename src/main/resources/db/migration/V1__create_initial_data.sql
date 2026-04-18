-- Roles
INSERT INTO roles (role_id, name, description, created_at)
VALUES
    ('a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b', 'ADMIN',   'Dueño de la plataforma',    NOW()),
    ('9b1d5f73-2a8e-4c6d-b394-7e0f2a5c8d1e', 'USER',    'Administrador de empresa',  NOW()),
    ('4c7a9e25-1b3f-4d8c-a762-0e5b8f2d4c9a', 'ADVISER', 'Asesor del negocio',        NOW())
    ON CONFLICT (name) DO NOTHING;

-- Modules
INSERT INTO modules (module_id, name, route, icon, display_order)
VALUES
    ('6e2f8b14-3d7a-4c9e-b5f2-8a1d0e7c3b6f', 'Dashboard',       'dashboard',       'dashboard',       1),
    ('2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', 'Bots',            'bots',            'smart_toy',        2),
    ('7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', 'Leads',           'leads',           'person_search',     3),
    ('1b8e4f27-5c2a-4d9b-a631-7f0e3c5b8d4a', 'Conversaciones',   'conversations',   'chat_bubble_outline',    4),
    ('5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', 'Documentos',       'documents',       'description',  5),
    ('3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', 'Usuarios',        'users',        'group',      6),
    ('8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', 'WhatsApp', 'whatsapp-config', 'smartphone', 7),
    ('4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', 'Negocios',         'tenants',         'business',   8),
    ('0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', 'Roles',           'roles',           'admin_panel_settings',     9)
    ON CONFLICT (name) DO NOTHING;

-- Permissions
INSERT INTO permissions (permission_id, module_id, action)
VALUES
    ('c3a7f291-5b8d-4e6c-a142-9f0d3b5e7c2a', '6e2f8b14-3d7a-4c9e-b5f2-8a1d0e7c3b6f', 'VIEW'),
    ('7e1b4f83-2c9a-4d5e-b637-0f4a8c2e1b6d', '2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', 'VIEW'),
    ('2f8c5d14-9a3b-4e7f-c261-6d5b0a9f3c8e', '2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', 'CREATE'),
    ('8a3e6b27-1f4c-4d9a-b583-2e7c1f0d4b5a', '2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', 'EDIT'),
    ('5b9f2c48-7d1a-4e3b-a896-3c4f8e2d0b7a', '2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', 'DELETE'),
    ('1d4a7e59-3c8f-4b2d-b614-7a5e9c1f3d0b', '7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', 'VIEW'),
    ('9f2d5b63-8a4c-4e1f-c729-4b6d0e3a8c2f', '7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', 'EDIT'),
    ('4c8e1f74-2b7d-4a5c-b843-8f1a6d2e5b9c', '7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', 'DELETE'),
    ('6a1c9d85-5f3b-4e8a-b256-1d9c4f7b2e0a', '1b8e4f27-5c2a-4d9b-a631-7f0e3c5b8d4a', 'VIEW'),
    ('3b7f4e96-1a8c-4d2b-c671-5e2b9a1f6d4c', '5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', 'VIEW'),
    ('8e5a2d07-4c1f-4b9e-b134-2f7d5c8a0e3b', '5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', 'CREATE'),
    ('0f9b6c18-7d4a-4e3f-a892-6c1e0b4d7f5a', '5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', 'DELETE'),
    ('7d3e8f29-2b5c-4a1d-b967-9a4f2e6c1b0d', '3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', 'VIEW'),
    ('2c6d1b30-8f7a-4e5c-b241-3b8d9f0e4c6a', '3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', 'CREATE'),
    ('9a4f5c41-1e2b-4d8f-c583-7c3a0d1e9b4f', '3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', 'EDIT'),
    ('5f1a8d52-6c3e-4b7a-b916-1e5f4c2b0d8a', '3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', 'DELETE'),
    ('1e7b3f63-4a9d-4c2e-b357-8d2c5a6f1e0b', '8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', 'VIEW'),
    ('6b2e9c74-3f5a-4d1b-a624-4a7e1d3c8f2b', '8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', 'CREATE'),
    ('3c5f4a85-8b1e-4e6d-b793-2f9a7b4c0d1e', '8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', 'EDIT'),
    ('8d9a1b96-2c6f-4f3a-c168-5b4d2e9a3f7c', '8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', 'DELETE'),
    ('4e2c7f07-9a3b-4b8e-b435-7c1f6d0e5a2b', '4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', 'VIEW'),
    ('0a6d4e18-1f8c-4c5b-b872-3e9b1a7f2d4c', '4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', 'CREATE'),
    ('7f8b2d29-5e1a-4a9c-c241-9d3c4b8e0f6a', '4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', 'DELETE'),
    ('2b1f6c30-4d7e-4e4a-b596-1a8f3c2d7b0e', '0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', 'VIEW'),
    ('9c4a8e41-7b2f-4d3b-a913-6f2d0c5a4e1b', '0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', 'CREATE'),
    ('5a7d3f52-2e9b-4c6d-b248-4b1e8a0c6d3f', '0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', 'EDIT'),
    ('1f3b9a63-6c4d-4f7e-c582-8e5a2d4b1c0f', '0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', 'DELETE')
    ON CONFLICT (permission_id) DO NOTHING;

-- ADMIN: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b', permission_id
FROM permissions
    ON CONFLICT DO NOTHING;


DROP TABLE leads;
