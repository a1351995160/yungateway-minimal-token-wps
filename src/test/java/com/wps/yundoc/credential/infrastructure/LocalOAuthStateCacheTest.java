package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.OAuthState;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalOAuthStateCacheTest {

    @Test
    void evictsExpiredStateOnWrite() {
        LocalOAuthStateCache cache = new LocalOAuthStateCache(properties(10));
        cache.put(state("expired", OffsetDateTime.now().minusSeconds(1)));

        cache.put(state("active", OffsetDateTime.now().plusMinutes(5)));

        assertThat(cache.take("expired")).isEmpty();
        assertThat(cache.take("active")).isPresent();
    }

    @Test
    void boundsStoredStateCount() {
        LocalOAuthStateCache cache = new LocalOAuthStateCache(properties(1));

        cache.put(state("first", OffsetDateTime.now().plusMinutes(5)));
        cache.put(state("second", OffsetDateTime.now().plusMinutes(5)));

        assertThat(cache.size()).isLessThanOrEqualTo(1);
    }

    private WpsUserAuthorizationProperties properties(int maxStateCount) {
        WpsUserAuthorizationProperties properties = new WpsUserAuthorizationProperties();
        properties.setMaxStateCount(maxStateCount);
        return properties;
    }

    private OAuthState state(String value, OffsetDateTime expiresAt) {
        return new OAuthState(value, "user-001", "biz-001", expiresAt);
    }
}
