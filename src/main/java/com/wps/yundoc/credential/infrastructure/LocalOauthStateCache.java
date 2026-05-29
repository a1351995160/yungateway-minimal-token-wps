package com.wps.yundoc.credential.infrastructure;

import com.wps.yundoc.credential.domain.OauthState;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LocalOauthStateCache component.
 *
 * @author WPS
 */
@Component
public class LocalOauthStateCache {

    private final ConcurrentMap<String, OauthState> states = new ConcurrentHashMap<>();
    private final WpsUserAuthorizationProperties properties;

    public LocalOauthStateCache(WpsUserAuthorizationProperties properties) {
        this.properties = properties;
    }

    public void put(OauthState state) {
        evictExpiredStates();
        states.put(state.getState(), state);
        evictOverflow();
    }

    public Optional<OauthState> take(String stateValue) {
        OauthState state = states.remove(stateValue);
        if (state == null) {
            return Optional.empty();
        }
        return validState(state);
    }

    int size() {
        return states.size();
    }

    private Optional<OauthState> validState(OauthState state) {
        if (state.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    private void evictExpiredStates() {
        for (Map.Entry<String, OauthState> entry : states.entrySet()) {
            evictExpiredState(entry);
        }
    }

    private void evictExpiredState(Map.Entry<String, OauthState> entry) {
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
