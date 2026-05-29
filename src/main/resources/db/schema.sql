CREATE TABLE IF NOT EXISTS biz_system (
    business_system_id VARCHAR(64) NOT NULL COMMENT 'business system stable id',
    business_system_name VARCHAR(128) NOT NULL COMMENT 'business system display name',
    client_id VARCHAR(64) NOT NULL COMMENT 'issued client id',
    client_secret_digest CHAR(64) NOT NULL COMMENT 'client secret digest',
    client_secret_salt VARCHAR(64) NOT NULL COMMENT 'client secret salt',
    client_secret_alg VARCHAR(32) NOT NULL COMMENT 'client secret digest algorithm',
    status VARCHAR(16) NOT NULL COMMENT 'ENABLED or DISABLED',
    token_version INT UNSIGNED NOT NULL COMMENT 'jwt token version',
    permission_version INT UNSIGNED NOT NULL COMMENT 'api permission version',
    jwt_ttl_seconds INT UNSIGNED NOT NULL COMMENT 'jwt ttl seconds',
    description VARCHAR(255) NULL COMMENT 'business system description',
    created_at DATETIME(3) NOT NULL COMMENT 'created time',
    updated_at DATETIME(3) NOT NULL COMMENT 'updated time',
    PRIMARY KEY (business_system_id),
    UNIQUE KEY uk_biz_system_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='business system access configuration';

CREATE TABLE IF NOT EXISTS biz_system_api_permission (
    business_system_id VARCHAR(64) NOT NULL COMMENT 'business system stable id',
    api_code VARCHAR(64) NOT NULL COMMENT 'gateway api permission code',
    status VARCHAR(16) NOT NULL COMMENT 'ENABLED or DISABLED',
    created_at DATETIME(3) NOT NULL COMMENT 'created time',
    updated_at DATETIME(3) NOT NULL COMMENT 'updated time',
    PRIMARY KEY (business_system_id, api_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='business system api permission';
