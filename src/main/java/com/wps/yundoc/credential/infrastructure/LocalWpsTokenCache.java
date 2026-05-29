package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.WpsCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LocalWpsTokenCache component.
 *
 * @author WPS
 */
@Component
public class LocalWpsTokenCache {

    private final AtomicReference<WpsCredential> cached = new AtomicReference<>();
    private final Duration refreshSkew;

    @Autowired
    public LocalWpsTokenCache(WpsCredentialProperties properties) {
        this(properties.getRefreshSkew());
    }

    public LocalWpsTokenCache(Duration refreshSkew) {
        this.refreshSkew = refreshSkew;
    }

    public Optional<WpsCredential> get() {
        WpsCredential credential = cached.get();
        if (credential == null) {
            return Optional.empty();
        }
        return validCredential(credential);
    }

    public void put(WpsCredential credential) {
        cached.set(credential);
    }

    private Optional<WpsCredential> validCredential(WpsCredential credential) {
        if (shouldRefresh(credential)) {
            cached.compareAndSet(credential, null);
            return Optional.empty();
        }
        return Optional.of(credential);
    }

    private boolean shouldRefresh(WpsCredential credential) {
        OffsetDateTime refreshAt = OffsetDateTime.now().plus(refreshSkew);
        return !credential.getExpiresAt().isAfter(refreshAt);
    }
}
