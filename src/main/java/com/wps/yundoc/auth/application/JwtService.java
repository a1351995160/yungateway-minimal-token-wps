package com.wps.yundoc.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.auth.infrastructure.JwtProperties;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JwtService component.
 *
 * @author WPS
 */
@Service
public class JwtService {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String JCA_HMAC_SHA256 = "HmacSHA256";
    private static final String TOKEN_TYPE = "business-jwt";
    private static final int JWT_PART_COUNT = 3;
    private static final String ISSUER_CLAIM = "iss";
    private static final String AUDIENCE_CLAIM = "aud";
    private static final String TYPE_CLAIM = "typ";
    private static final String BUSINESS_SYSTEM_ID_CLAIM = "businessSystemId";
    private static final String CLIENT_ID_CLAIM = "clientId";
    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";
    private static final String PERMISSION_VERSION_CLAIM = "permissionVersion";
    private static final String JWT_ID_CLAIM = "jti";
    private static final String ISSUED_AT_CLAIM = "iat";
    private static final String EXPIRES_AT_CLAIM = "exp";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String issue(BusinessSystemPrincipal principal) {
        return issue(principal, properties.getTtl().getSeconds());
    }

    public String issue(BusinessSystemPrincipal principal, long ttlSeconds) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + ttlSeconds;
        String payload = encodeJson(payload(principal, issuedAt, expiresAt));
        String signingInput = base64Url(HEADER) + "." + payload;
        return signingInput + "." + signature(signingInput);
    }

    public BusinessSystemPrincipal validate(String token) {
        String[] parts = token.split("\\.");
        validateFormat(parts);
        validateSignature(parts);
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
        payload.put(BUSINESS_SYSTEM_ID_CLAIM, principal.getBusinessSystemId());
        payload.put(CLIENT_ID_CLAIM, principal.getClientId());
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

    private void validateSignature(String[] parts) {
        String signingInput = parts[0] + "." + parts[1];
        byte[] actual = signature(signingInput).getBytes(StandardCharsets.UTF_8);
        byte[] expected = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(actual, expected)) {
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
        return new BusinessSystemPrincipal(
                payload.path(BUSINESS_SYSTEM_ID_CLAIM).asText(),
                payload.path(CLIENT_ID_CLAIM).asText(),
                payload.path(JWT_ID_CLAIM).asText(),
                payload.path(TOKEN_VERSION_CLAIM).asInt(),
                payload.path(PERMISSION_VERSION_CLAIM).asInt());
    }

    private JsonNode readPayload(String encodedPayload) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readTree(json);
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64Url(objectMapper.writeValueAsString(value));
        } catch (java.io.IOException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Token creation failed", ex);
        }
    }

    private String signature(String signingInput) {
        try {
            Mac mac = Mac.getInstance(JCA_HMAC_SHA256);
            byte[] key = properties.getSecret().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, JCA_HMAC_SHA256));
            return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Token signing failed", ex);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
