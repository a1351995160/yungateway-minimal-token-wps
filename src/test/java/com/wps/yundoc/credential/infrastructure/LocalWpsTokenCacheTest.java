package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.WpsCredential;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalWpsTokenCacheTest {

    @Test
    void returnsCredentialBeforeRefreshSkew() {
        LocalWpsTokenCache cache = new LocalWpsTokenCache(Duration.ofSeconds(60));
        WpsCredential credential = credential(OffsetDateTime.now().plusMinutes(5));

        cache.put(credential);

        assertThat(cache.get()).contains(credential);
    }

    @Test
    void evictsCredentialInsideRefreshSkew() {
        LocalWpsTokenCache cache = new LocalWpsTokenCache(Duration.ofSeconds(60));
        cache.put(credential(OffsetDateTime.now().plusSeconds(30)));

        assertThat(cache.get()).isEmpty();
    }

    private WpsCredential credential(OffsetDateTime expiresAt) {
        return new WpsCredential("app-token", expiresAt);
    }
}
