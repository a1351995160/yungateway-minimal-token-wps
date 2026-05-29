package com.wps.yundoc.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.auth.infrastructure.JwtProperties;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void rejectsExpiredBusinessJwt() {
        JwtService service = new JwtService(properties(Duration.ofSeconds(-1)), new ObjectMapper());
        String token = service.issue(principal());

        assertThatThrownBy(() -> service.validate(token))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.TOKEN_INVALID);
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
}
