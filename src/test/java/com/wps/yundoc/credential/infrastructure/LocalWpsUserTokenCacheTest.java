package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalWpsUserTokenCacheTest {

    @Test
    void evictsExpiredTokenOnWrite() {
        LocalWpsUserTokenCache cache = new LocalWpsUserTokenCache(properties(10));
        cache.put("expired-user", token(OffsetDateTime.now().minusSeconds(1)));

        cache.put("active-user", token(OffsetDateTime.now().plusMinutes(5)));

        assertThat(cache.get("expired-user")).isEmpty();
        assertThat(cache.get("active-user")).isPresent();
    }

    @Test
    void boundsStoredTokenCount() {
        LocalWpsUserTokenCache cache = new LocalWpsUserTokenCache(properties(1));

        cache.put("user-001", token(OffsetDateTime.now().plusMinutes(5)));
        cache.put("user-002", token(OffsetDateTime.now().plusMinutes(5)));

        assertThat(cache.size()).isLessThanOrEqualTo(1);
    }

    private WpsCredentialProperties properties(int maxTokenCount) {
        WpsCredentialProperties properties = new WpsCredentialProperties();
        properties.setRefreshSkew(Duration.ZERO);
        properties.setMaxUserTokenCount(maxTokenCount);
        return properties;
    }

    private WpsUserToken token(OffsetDateTime expiresAt) {
        return new WpsUserToken("user-token", expiresAt);
    }
}
