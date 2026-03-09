-- Insert master realm
INSERT INTO realms (id, name, display_name, enabled, registration_allowed, access_token_lifespan, refresh_token_lifespan)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'master',
    'Master Realm',
    TRUE,
    TRUE,
    300,
    1800
) ON CONFLICT (name) DO NOTHING;

-- Insert admin-cli client
INSERT INTO clients (realm_id, client_id, client_secret, name, description, enabled, public_client, grant_types)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin-cli',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCMc3vCYY8bJ9t3K/6',
    'Admin CLI',
    'Admin command-line client',
    TRUE,
    FALSE,
    '["password","client_credentials","refresh_token"]'
) ON CONFLICT (realm_id, client_id) DO NOTHING;

-- Insert account client (public)
INSERT INTO clients (realm_id, client_id, name, description, enabled, public_client, redirect_uris, grant_types)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'account',
    'Account',
    'Default public client for web apps',
    TRUE,
    TRUE,
    '["http://localhost:3000/*","http://localhost:5173/*"]',
    '["authorization_code","refresh_token"]'
) ON CONFLICT (realm_id, client_id) DO NOTHING;
