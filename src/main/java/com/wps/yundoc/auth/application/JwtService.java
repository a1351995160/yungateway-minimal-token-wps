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

@Service
public class JwtService {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String JCA_HMAC_SHA256 = "HmacSHA256";
    private static final String TOKEN_TYPE = "business-jwt";

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
        payload.put("iss", properties.getIssuer());
        payload.put("aud", properties.getAudience());
        payload.put("typ", TOKEN_TYPE);
        payload.put("businessSystemId", principal.getBusinessSystemId());
        payload.put("clientId", principal.getClientId());
        payload.put("tokenVersion", principal.getTokenVersion());
        payload.put("permissionVersion", principal.getPermissionVersion());
        payload.put("jti", principal.getJti());
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);
        return payload;
    }

    private void validateFormat(String[] parts) {
        if (parts.length != 3) {
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
        if (!properties.getIssuer().equals(payload.path("iss").asText())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validateAudience(JsonNode payload) {
        if (!properties.getAudience().equals(payload.path("aud").asText())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validateType(JsonNode payload) {
        if (!TOKEN_TYPE.equals(payload.path("typ").asText())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void validateExpiry(JsonNode payload) {
        if (payload.path("exp").asLong() <= Instant.now().getEpochSecond()) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private BusinessSystemPrincipal principal(JsonNode payload) {
        return new BusinessSystemPrincipal(
                payload.path("businessSystemId").asText(),
                payload.path("clientId").asText(),
                payload.path("jti").asText(),
                payload.path("tokenVersion").asInt(),
                payload.path("permissionVersion").asInt());
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
