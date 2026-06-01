package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenRateLimiterTest {

    @Test
    void rateLimitsByRemoteAddressWhenClientIdsAreRotated() {
        AuthTokenRateLimitProperties properties = properties();
        properties.setMaxFailuresPerClient(100);
        properties.setMaxFailuresPerRemoteAddress(2);
        AuthTokenRateLimiter limiter = limiter(properties);

        limiter.recordFailure("client-a", "10.0.0.1");
        limiter.recordFailure("client-b", "10.0.0.1");

        assertThatThrownBy(() -> limiter.assertAllowed("client-c", "10.0.0.1"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    void successClearsClientFailureCount() {
        AuthTokenRateLimitProperties properties = properties();
        properties.setMaxFailuresPerClient(2);
        AuthTokenRateLimiter limiter = limiter(properties);

        limiter.recordFailure("client-a", "10.0.0.1");
        limiter.recordSuccess("client-a");
        limiter.recordFailure("client-a", "10.0.0.1");

        assertThatCode(() -> limiter.assertAllowed("client-a", "10.0.0.1"))
                .doesNotThrowAnyException();
    }

    private AuthTokenRateLimiter limiter(AuthTokenRateLimitProperties properties) {
        Clock clock = Clock.fixed(Instant.parse("2026-05-30T00:00:00Z"), ZoneOffset.UTC);
        return new AuthTokenRateLimiter(properties, clock);
    }

    private AuthTokenRateLimitProperties properties() {
        AuthTokenRateLimitProperties properties = new AuthTokenRateLimitProperties();
        properties.setWindow(Duration.ofMinutes(1));
        properties.setMaxFailuresPerClient(5);
        properties.setMaxFailuresPerRemoteAddress(20);
        return properties;
    }
}
