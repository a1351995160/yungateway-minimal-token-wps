package com.wps.yundoc.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.auth.infrastructure.JwtProperties;
import com.wps.yundoc.businesssystem.domain.WpsIdentityType;
import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
import com.wps.yundoc.common.crypto.YundocCryptoService;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * JwtService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class JwtService {

    private static final String TOKEN_TYPE = "business-jwt";
    private static final int JWT_PART_COUNT = 3;
    private static final String ALGORITHM_CLAIM = "alg";
    private static final String ISSUER_CLAIM = "iss";
    private static final String AUDIENCE_CLAIM = "aud";
    private static final String TYPE_CLAIM = "typ";
    private static final String IDENTITY_TYPE_CLAIM = "identityType";
    private static final String BUSINESS_SYSTEM_ID_CLAIM = "businessSystemId";
    private static final String CLIENT_ID_CLAIM = "clientId";
    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";
    private static final String PERMISSION_VERSION_CLAIM = "permissionVersion";
    private static final String JWT_ID_CLAIM = "jti";
    private static final String ISSUED_AT_CLAIM = "iat";
    private static final String EXPIRES_AT_CLAIM = "exp";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;
    private final YundocCryptoService cryptoService;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper, YundocCryptoService cryptoService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
    }

    public String issue(BusinessSystemPrincipal principal) {
        return issue(principal, properties.getTtl().getSeconds());
    }

    public String issue(BusinessSystemPrincipal principal, long ttlSeconds) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + ttlSeconds;
        String payload = encodeJson(payload(principal, issuedAt, expiresAt));
        String signingInput = encodeJson(header()) + "." + payload;
        return signingInput + "." + signature(properties.getAlgorithm(), signingInput);
    }

    public BusinessSystemPrincipal validate(String token) {
        String[] parts = token.split("\\.");
        validateFormat(parts);
        String algorithm = validateHeader(parts[0]);
        validateSignature(parts, algorithm);
        JsonNode payload = readPayload(parts[1]);
        validatePayload(payload);
        return principal(payload);
    }

    public long expiresInSeconds() {
        return properties.getTtl().getSeconds();
    }

    private Map<String, Object> payload(BusinessSystemPrincipal principal, long issuedAt, long expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(ISSUER_CLAIM, properties.getIssuer());
        payload.put(AUDIENCE_CLAIM, properties.getAudience());
        payload.put(TYPE_CLAIM, TOKEN_TYPE);
        payload.put(IDENTITY_TYPE_CLAIM, principal.getIdentityType().name());
        payload.put(BUSINESS_SYSTEM_ID_CLAIM, principal.getBusinessSystemId());
        payload.put(CLIENT_ID_CLAIM, principal.getClientId());
        if (principal.getIdentityType() == WpsIdentityType.USER) {
            payload.put(USER_ID_CLAIM, principal.getUserId());
        }
        payload.put(TOKEN_VERSION_CLAIM, principal.getTokenVersion());
        payload.put(PERMISSION_VERSION_CLAIM, principal.getPermissionVersion());
        payload.put(JWT_ID_CLAIM, principal.getJti());
        payload.put(ISSUED_AT_CLAIM, issuedAt);
        payload.put(EXPIRES_AT_CLAIM, expiresAt);
        return payload;
    }

    private void validateFormat(String[] parts) {
        if (parts.length != JWT_PART_COUNT) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private String validateHeader(String encodedHeader) {
        JsonNode header = readJson(encodedHeader);
        String algorithm = header.path(ALGORITHM_CLAIM).asText();
        if (supportsAlgorithm(algorithm)) {
            return normalizedAlgorithm(algorithm);
        }
        throw new YundocException(YundocErrorCode.TOKEN_INVALID);
    }

    private void validateSignature(String[] parts, String algorithm) {
        String signingInput = parts[0] + "." + parts[1];
        byte[] actual = signature(algorithm, signingInput).getBytes(StandardCharsets.UTF_8);
        byte[] expected = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!cryptoService.matches(actual, expected)) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validatePayload(JsonNode payload) {
        validateIssuer(payload);
        validateAudience(payload);
        validateType(payload);
        validateExpiry(payload);
    }

    private void validateIssuer(JsonNode payload) {
        if (!properties.getIssuer().equals(payload.path(ISSUER_CLAIM).asText())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validateAudience(JsonNode payload) {
        if (!properties.getAudience().equals(payload.path(AUDIENCE_CLAIM).asText())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validateType(JsonNode payload) {
        if (!TOKEN_TYPE.equals(payload.path(TYPE_CLAIM).asText())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validateExpiry(JsonNode payload) {
        if (payload.path(EXPIRES_AT_CLAIM).asLong() <= Instant.now().getEpochSecond()) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private BusinessSystemPrincipal principal(JsonNode payload) {
        WpsIdentityType identityType = identityType(payload);
        return BusinessSystemPrincipal.builder()
                .businessSystemId(payload.path(BUSINESS_SYSTEM_ID_CLAIM).asText())
                .clientId(payload.path(CLIENT_ID_CLAIM).asText())
                .identityType(identityType)
                .userId(userId(payload, identityType))
                .jti(payload.path(JWT_ID_CLAIM).asText())
                .tokenVersion(payload.path(TOKEN_VERSION_CLAIM).asInt())
                .permissionVersion(payload.path(PERMISSION_VERSION_CLAIM).asInt())
                .build();
    }

    private WpsIdentityType identityType(JsonNode payload) {
        if (!payload.hasNonNull(IDENTITY_TYPE_CLAIM)) {
            return WpsIdentityType.APP;
        }
        try {
            return WpsIdentityType.valueOf(payload.path(IDENTITY_TYPE_CLAIM).asText());
        } catch (IllegalArgumentException ex) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private String userId(JsonNode payload, WpsIdentityType identityType) {
        if (identityType == WpsIdentityType.APP) {
            return null;
        }
        String userId = payload.path(USER_ID_CLAIM).asText();
        if (userId.isEmpty()) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
        return userId;
    }

    private JsonNode readPayload(String encodedPayload) {
        return readJson(encodedPayload);
    }

    private JsonNode readJson(String encodedJson) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedJson);
            return objectMapper.readTree(json);
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private Map<String, Object> header() {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put(ALGORITHM_CLAIM, normalizedAlgorithm(properties.getAlgorithm()));
        header.put(TYPE_CLAIM, "JWT");
        return header;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64Url(objectMapper.writeValueAsString(value));
        } catch (java.io.IOException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Token creation failed", ex);
        }
    }

    private String signature(String algorithm, String signingInput) {
        return cryptoService.hmacBase64Url(normalizedAlgorithm(algorithm), properties.getSecret(), signingInput);
    }

    private boolean supportsAlgorithm(String algorithm) {
        String normalized = normalizedAlgorithm(algorithm);
        if (YundocCryptoAlgorithms.JWT_HSM3.equals(normalized)) {
            return true;
        }
        return properties.isLegacyValidationEnabled()
                && YundocCryptoAlgorithms.JWT_HS256.equals(normalized);
    }

    private String normalizedAlgorithm(String algorithm) {
        if (!Texts.hasText(algorithm)) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
        return algorithm.trim().toUpperCase(Locale.ROOT);
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

}
