package com.wps.yundoc.credential.domain;

import java.time.OffsetDateTime;

/**
 * OauthState component.
 *
 * @author WPS
 */
public class OauthState {

    private final String state;
    private final String userId;
    private final String businessSystemId;
    private final OffsetDateTime expiresAt;

    public OauthState(String state, String userId, String businessSystemId, OffsetDateTime expiresAt) {
        this.state = state;
        this.userId = userId;
        this.businessSystemId = businessSystemId;
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

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
