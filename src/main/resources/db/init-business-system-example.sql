-- Example seed for local development.
-- Runtime flow:
-- 1. Insert a business system into biz_system.
-- 2. Insert allowed WPS capability codes into biz_system_api_permission.
-- 3. Call POST /api/v1/auth/token with client_id and the raw client secret.
--
-- The digest below is for:
-- client_secret = local-client-secret
-- salt = local-salt
-- yundoc.client-secret.pepper = local-client-secret-pepper
-- algorithm = HMAC-SHA256

DELETE FROM biz_system_api_permission WHERE business_system_id = 'biz_local_demo';
DELETE FROM biz_system WHERE business_system_id = 'biz_local_demo' OR client_id = 'local-client';

INSERT INTO biz_system (
    business_system_id,
    business_system_name,
    client_id,
    client_secret_digest,
    client_secret_salt,
    client_secret_alg,
    user_assertion_public_key,
    status,
    token_version,
    permission_version,
    jwt_ttl_seconds,
    description,
    created_at,
    updated_at
) VALUES (
    'biz_local_demo',
    'Local Demo Business System',
    'local-client',
    'b0ad8e6e5ec0b0999474c06f8e5c79620b9e4a8f44c0896d481cf84da09cad1a',
    'local-salt',
    'HMAC-SHA256',
    NULL,
    'ENABLED',
    1,
    1,
    1800,
    'Seeded by db/init-business-system-example.sql',
    NOW(3),
    NOW(3)
);

INSERT INTO biz_system_api_permission (
    business_system_id,
    api_code,
    status,
    created_at,
    updated_at
) VALUES
    ('biz_local_demo', 'app-preview:create', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:list', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:rename', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:download', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-folders:rename', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:create', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:save-as', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:view', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:delete', 'ENABLED', NOW(3), NOW(3)),
    ('biz_local_demo', 'user-files:update', 'ENABLED', NOW(3), NOW(3));
