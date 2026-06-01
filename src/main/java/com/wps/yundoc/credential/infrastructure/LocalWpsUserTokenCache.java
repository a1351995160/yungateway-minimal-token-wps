package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalWpsUserTokenCache component.
 *
 * @author WPS
 */
@Component
public class LocalWpsUserTokenCache {

    private final ConcurrentMap<String, WpsUserToken> tokens = new ConcurrentHashMap<>();
    private final WpsCredentialProperties properties;

    public LocalWpsUserTokenCache(WpsCredentialProperties properties) {
        this.properties = properties;
    }

    public Optional<WpsUserToken> get(String userId) {
        WpsUserToken token = tokens.get(userId);
        if (token == null) {
            return Optional.empty();
        }
        return validToken(userId, token);
    }

    public void put(String userId, WpsUserToken token) {
        evictExpiredTokens();
        tokens.put(userId, token);
        evictOverflow();
    }

    public void remove(String userId) {
        tokens.remove(userId);
    }

    public void remove(String userId, WpsUserToken token) {
        tokens.remove(userId, token);
    }

    int size() {
        return tokens.size();
    }

    private Optional<WpsUserToken> validToken(String userId, WpsUserToken token) {
        if (shouldRemove(token)) {
            tokens.remove(userId, token);
            return Optional.empty();
        }
        return Optional.of(token);
    }

    private boolean shouldRefresh(WpsUserToken token) {
        OffsetDateTime refreshAt = OffsetDateTime.now().plus(properties.getRefreshSkew());
        return !token.getExpiresAt().isAfter(refreshAt);
    }

    private void evictExpiredTokens() {
        for (Map.Entry<String, WpsUserToken> entry : tokens.entrySet()) {
            evictExpiredToken(entry);
        }
    }

    private void evictExpiredToken(Map.Entry<String, WpsUserToken> entry) {
        if (shouldRemove(entry.getValue())) {
            tokens.remove(entry.getKey(), entry.getValue());
        }
    }

    private boolean shouldRemove(WpsUserToken token) {
        if (!shouldRefresh(token)) {
            return false;
        }
        return token.getRefreshToken() == null
                || token.getRefreshExpiresAt() == null
                || !token.getRefreshExpiresAt().isAfter(OffsetDateTime.now());
    }

    private void evictOverflow() {
        while (tokens.size() > maxTokenCount()) {
            removeOneToken();
        }
    }

    private int maxTokenCount() {
        return Math.max(1, properties.getMaxUserTokenCount());
    }

    private void removeOneToken() {
        tokens.keySet().stream()
                .findFirst()
                .ifPresent(tokens::remove);
    }
}
