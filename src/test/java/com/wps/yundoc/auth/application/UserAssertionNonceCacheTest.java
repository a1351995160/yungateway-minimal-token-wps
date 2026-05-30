package com.wps.yundoc.auth.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserAssertionNonceCacheTest {

    @Test
    void rejectsNewNonceWhenCapacityIsFullWithoutEvictingLiveNonce() {
        UserAssertionProperties properties = new UserAssertionProperties();
        properties.setMaxTrackedNonces(1);
        UserAssertionNonceCache cache = new UserAssertionNonceCache(properties);
        long expiresAt = Instant.now().plusSeconds(60).getEpochSecond();

        assertThat(cache.markUsed("biz-001", "first", expiresAt)).isTrue();
        assertThat(cache.markUsed("biz-001", "second", expiresAt)).isFalse();
        assertThat(cache.markUsed("biz-001", "first", expiresAt)).isFalse();
    }

    @Test
    void acceptsNewNonceAfterExpiredEntriesAreEvicted() {
        UserAssertionProperties properties = new UserAssertionProperties();
        properties.setMaxTrackedNonces(1);
        UserAssertionNonceCache cache = new UserAssertionNonceCache(properties);
        long expiredAt = Instant.now().minusSeconds(1).getEpochSecond();
        long expiresAt = Instant.now().plusSeconds(60).getEpochSecond();

        assertThat(cache.markUsed("biz-001", "first", expiredAt)).isTrue();
        assertThat(cache.markUsed("biz-001", "second", expiresAt)).isTrue();
    }
}
