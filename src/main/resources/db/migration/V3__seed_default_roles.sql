-- Insert default roles for master realm
INSERT INTO roles (id, realm_id, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 'admin', 'Administrator role with full access'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'user', 'Default user role')
ON CONFLICT (realm_id, name) DO NOTHING;
