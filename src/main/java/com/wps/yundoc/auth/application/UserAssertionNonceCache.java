package com.wps.yundoc.auth.application;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * UserAssertionNonceCache component.
 *
 * @author WPS
 */
@Component
public class UserAssertionNonceCache {

    private final ConcurrentMap<String, Long> nonces = new ConcurrentHashMap<>();
    private final UserAssertionProperties properties;

    public UserAssertionNonceCache(UserAssertionProperties properties) {
        this.properties = properties;
    }

    public boolean markUsed(String businessSystemId, String nonce, long expiresAtEpochSecond) {
        evictExpired();
        Long existing = nonces.putIfAbsent(key(businessSystemId, nonce), Long.valueOf(expiresAtEpochSecond));
        evictOverflow();
        return existing == null;
    }

    private void evictExpired() {
        long now = Instant.now().getEpochSecond();
        for (Map.Entry<String, Long> entry : nonces.entrySet()) {
            if (entry.getValue().longValue() <= now) {
                nonces.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private void evictOverflow() {
        while (nonces.size() > maxTrackedNonces()) {
            nonces.keySet().stream()
                    .findFirst()
                    .ifPresent(nonces::remove);
        }
    }

    private int maxTrackedNonces() {
        return Math.max(1, properties.getMaxTrackedNonces());
    }

    private String key(String businessSystemId, String nonce) {
        return businessSystemId + ":" + nonce;
    }
}
