package com.wps.yundoc.testsupport;

import com.wps.yundoc.auth.application.ClientSecretDigestService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class BusinessSystemFixture {

    private static final String DIGEST_ALGORITHM = "HMAC-SHA256";
    private static final String DEFAULT_SECRET = "test-client-secret";
    private static final int DEFAULT_JWT_TTL_SECONDS = 1800;

    private final JdbcTemplate jdbcTemplate;
    private final ClientSecretDigestService digestService;

    public BusinessSystemFixture(JdbcTemplate jdbcTemplate, ClientSecretDigestService digestService) {
        this.jdbcTemplate = jdbcTemplate;
        this.digestService = digestService;
    }

    public BusinessSystemCredentials enabled(String businessSystemId, String... apiCodes) {
        return enabled(businessSystemId, DEFAULT_JWT_TTL_SECONDS, apiCodes);
    }

    public BusinessSystemCredentials enabled(
            String businessSystemId,
            int jwtTtlSeconds,
            String... apiCodes) {
        return save(businessSystemId, "ENABLED", DEFAULT_SECRET, jwtTtlSeconds, apiCodes);
    }

    public BusinessSystemCredentials disabled(String businessSystemId) {
        return save(businessSystemId, "DISABLED", DEFAULT_SECRET, DEFAULT_JWT_TTL_SECONDS);
    }

    public void replacePermissions(String businessSystemId, String... apiCodes) {
        jdbcTemplate.update(
                "DELETE FROM biz_system_api_permission WHERE business_system_id = ?",
                businessSystemId);
        Arrays.stream(apiCodes).forEach(apiCode -> insertPermission(businessSystemId, apiCode));
        jdbcTemplate.update(
                "UPDATE biz_system SET permission_version = permission_version + 1, updated_at = ? "
                        + "WHERE business_system_id = ?",
                LocalDateTime.now(),
                businessSystemId);
    }

    public void rotateClientSecret(String businessSystemId) {
        jdbcTemplate.update(
                "UPDATE biz_system SET token_version = token_version + 1, updated_at = ? "
                        + "WHERE business_system_id = ?",
                LocalDateTime.now(),
                businessSystemId);
    }

    private BusinessSystemCredentials save(
            String businessSystemId,
            String status,
            String clientSecret,
            int jwtTtlSeconds,
            String... apiCodes) {
        String clientId = "cli-" + businessSystemId;
        String salt = "salt-" + businessSystemId;
        String digest = digestService.digest(clientSecret, salt, DIGEST_ALGORITHM);
        LocalDateTime now = LocalDateTime.now();
        deleteExisting(businessSystemId, clientId);
        jdbcTemplate.update(
                "INSERT INTO biz_system ("
                        + "business_system_id, business_system_name, client_id, "
                        + "client_secret_digest, client_secret_salt, client_secret_alg, "
                        + "status, token_version, permission_version, jwt_ttl_seconds, "
                        + "description, created_at, updated_at"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                businessSystemId,
                "Test Business System",
                clientId,
                digest,
                salt,
                DIGEST_ALGORITHM,
                status,
                1,
                1,
                jwtTtlSeconds,
                "Inserted by test fixture",
                now,
                now);
        Arrays.stream(apiCodes).forEach(apiCode -> insertPermission(businessSystemId, apiCode));
        return new BusinessSystemCredentials(businessSystemId, clientId, clientSecret);
    }

    private void deleteExisting(String businessSystemId, String clientId) {
        jdbcTemplate.update(
                "DELETE FROM biz_system_api_permission WHERE business_system_id = ?",
                businessSystemId);
        jdbcTemplate.update(
                "DELETE FROM biz_system WHERE business_system_id = ? OR client_id = ?",
                businessSystemId,
                clientId);
    }

    private void insertPermission(String businessSystemId, String apiCode) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO biz_system_api_permission ("
                        + "business_system_id, api_code, status, created_at, updated_at"
                        + ") VALUES (?, ?, ?, ?, ?)",
                businessSystemId,
                apiCode,
                "ENABLED",
                now,
                now);
    }
}
