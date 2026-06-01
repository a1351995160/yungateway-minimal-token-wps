package com.wps.yundoc.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.auth.infrastructure.JwtProperties;
import com.wps.yundoc.businesssystem.domain.WpsIdentityType;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsExpiredBusinessJwt() {
        JwtService service = new JwtService(properties(Duration.ofSeconds(-1)), objectMapper);
        String token = service.issue(principal());

        assertThatThrownBy(() -> service.validate(token))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.TOKEN_INVALID);
    }

    @Test
    void treatsLegacyJwtWithoutIdentityTypeAsAppToken() throws Exception {
        JwtProperties properties = properties(Duration.ofMinutes(30));
        JwtService service = new JwtService(properties, objectMapper);
        String legacyToken = legacyAppToken(properties);

        BusinessSystemPrincipal principal = service.validate(legacyToken);

        assertThat(principal.getIdentityType()).isEqualTo(WpsIdentityType.APP);
        assertThat(principal.getUserId()).isNull();
        assertThat(principal.getBusinessSystemId()).isEqualTo("biz-legacy");
    }

    private JwtProperties properties(Duration ttl) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("issuer");
        properties.setAudience("audience");
        properties.setSecret("test-business-jwt-secret-with-enough-length");
        properties.setTtl(ttl);
        return properties;
    }

    private BusinessSystemPrincipal principal() {
        return new BusinessSystemPrincipal(
                "biz-expired",
                "client-expired",
                "jti-expired",
                1,
                1);
    }

    private String legacyAppToken(JwtProperties properties) throws Exception {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(objectMapper.writeValueAsString(legacyPayload(properties)));
        String signingInput = header + "." + payload;
        return signingInput + "." + signature(properties, signingInput);
    }

    private Map<String, Object> legacyPayload(JwtProperties properties) {
        long issuedAt = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("aud", properties.getAudience());
        payload.put("typ", "business-jwt");
        payload.put("businessSystemId", "biz-legacy");
        payload.put("clientId", "client-legacy");
        payload.put("tokenVersion", 1);
        payload.put("permissionVersion", 1);
        payload.put("jti", "jti-legacy");
        payload.put("iat", issuedAt);
        payload.put("exp", issuedAt + properties.getTtl().getSeconds());
        return payload;
    }

    private String signature(JwtProperties properties, String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] key = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(String value) {
        return base64Url(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
