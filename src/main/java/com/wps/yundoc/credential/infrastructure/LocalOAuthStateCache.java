package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.OAuthState;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LocalOAuthStateCache {

    private final ConcurrentMap<String, OAuthState> states = new ConcurrentHashMap<>();
    private final WpsUserAuthorizationProperties properties;

    public LocalOAuthStateCache(WpsUserAuthorizationProperties properties) {
        this.properties = properties;
    }

    public void put(OAuthState state) {
        evictExpiredStates();
        states.put(state.getState(), state);
        evictOverflow();
    }

    public Optional<OAuthState> take(String stateValue) {
        OAuthState state = states.remove(stateValue);
        if (state == null) {
            return Optional.empty();
        }
        return validState(state);
    }

    int size() {
        return states.size();
    }

    private Optional<OAuthState> validState(OAuthState state) {
        if (state.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    private void evictExpiredStates() {
        for (Map.Entry<String, OAuthState> entry : states.entrySet()) {
            evictExpiredState(entry);
        }
    }

    private void evictExpiredState(Map.Entry<String, OAuthState> entry) {
        if (entry.getValue().getExpiresAt().isBefore(OffsetDateTime.now())) {
            states.remove(entry.getKey(), entry.getValue());
        }
    }

    private void evictOverflow() {
        while (states.size() > maxStateCount()) {
            removeOneState();
        }
    }

    private int maxStateCount() {
        return Math.max(1, properties.getMaxStateCount());
    }

    private void removeOneState() {
        states.keySet().stream()
                .findFirst()
                .ifPresent(states::remove);
    }
}
