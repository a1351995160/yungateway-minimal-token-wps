package com.wps.yundoc.credential.domain;

import java.time.OffsetDateTime;

/**
 * OauthState 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class OauthState {

    private final String state;
    private final String userId;
    private final String businessSystemId;
    private final String clientId;
    private final OffsetDateTime expiresAt;

    public OauthState(String state, String userId, String businessSystemId, OffsetDateTime expiresAt) {
        this(state, userId, businessSystemId, null, expiresAt);
    }

    public OauthState(
            String state,
            String userId,
            String businessSystemId,
            String clientId,
            OffsetDateTime expiresAt) {
        this.state = state;
        this.userId = userId;
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.expiresAt = expiresAt;
    }

    public String getState() {
        return state;
    }

    public String getUserId() {
        return userId;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public String getClientId() {
        return clientId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
