package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AuthTokenRateLimiter component.
 *
 * @author WPS
 */
@Component
public class AuthTokenRateLimiter {

    private static final String UNKNOWN_KEY = "unknown";

    private final AuthTokenRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, AttemptBucket> clientFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AttemptBucket> remoteAddressFailures = new ConcurrentHashMap<>();

    @Autowired
    public AuthTokenRateLimiter(AuthTokenRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AuthTokenRateLimiter(AuthTokenRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void assertAllowed(String clientId, String remoteAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        long now = clock.millis();
        assertAllowed(
                clientFailures,
                key(clientId),
                properties.getMaxFailuresPerClient(),
                now);
        assertAllowed(
                remoteAddressFailures,
                key(remoteAddress),
                properties.getMaxFailuresPerRemoteAddress(),
                now);
    }

    public void recordFailure(String clientId, String remoteAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        long now = clock.millis();
        recordFailure(clientFailures, key(clientId), now);
        recordFailure(remoteAddressFailures, key(remoteAddress), now);
    }

    public void recordSuccess(String clientId) {
        clientFailures.remove(key(clientId));
    }

    private void assertAllowed(
            ConcurrentMap<String, AttemptBucket> failures,
            String key,
            int maxFailures,
            long now) {
        if (maxFailures <= 0) {
            return;
        }
        AttemptBucket bucket = activeBucket(failures, key, now);
        if (bucketAllowed(bucket, maxFailures)) {
            return;
        }
        throw new YundocException(YundocErrorCode.RATE_LIMIT_EXCEEDED);
    }

    private AttemptBucket activeBucket(ConcurrentMap<String, AttemptBucket> failures, String key, long now) {
        AttemptBucket bucket = failures.get(key);
        if (bucketNotFound(bucket)) {
            return null;
        }
        return removeExpiredBucket(failures, key, bucket, now);
    }

    private boolean bucketNotFound(AttemptBucket bucket) {
        return bucket == null;
    }

    private AttemptBucket removeExpiredBucket(
            ConcurrentMap<String, AttemptBucket> failures,
            String key,
            AttemptBucket bucket,
            long now) {
        if (bucket.isExpired(now, windowMillis())) {
            failures.remove(key, bucket);
            return null;
        }
        return bucket;
    }

    private boolean bucketAllowed(AttemptBucket bucket, int maxFailures) {
        return bucket == null || bucket.getFailures() < maxFailures;
    }

    private void recordFailure(
            ConcurrentMap<String, AttemptBucket> failures,
            String key,
            long now) {
        cleanupIfNeeded(failures, now);
        failures.compute(key, (failureKey, bucket) -> {
            if (bucket == null || bucket.isExpired(now, windowMillis())) {
                return new AttemptBucket(now, 1);
            }
            return bucket.incremented();
        });
    }

    private void cleanupIfNeeded(ConcurrentMap<String, AttemptBucket> failures, long now) {
        if (failures.size() <= properties.getMaxTrackedKeys()) {
            return;
        }
        long windowMillis = windowMillis();
        failures.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMillis));
    }

    private long windowMillis() {
        if (properties.getWindow() == null) {
            return 0L;
        }
        return Math.max(0L, properties.getWindow().toMillis());
    }

    private String key(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN_KEY;
        }
        return value.trim();
    }

    private static class AttemptBucket {

        private final long windowStartedAtMillis;
        private final int failures;

        AttemptBucket(long windowStartedAtMillis, int failures) {
            this.windowStartedAtMillis = windowStartedAtMillis;
            this.failures = failures;
        }

        int getFailures() {
            return failures;
        }

        boolean isExpired(long now, long windowMillis) {
            return windowMillis <= 0L || now - windowStartedAtMillis >= windowMillis;
        }

        AttemptBucket incremented() {
            return new AttemptBucket(windowStartedAtMillis, failures + 1);
        }
    }
}
