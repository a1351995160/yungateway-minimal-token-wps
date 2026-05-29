package com.wps.yundoc.credential.domain;

import java.time.OffsetDateTime;

public class WpsUserToken {

    private final String accessToken;
    private final OffsetDateTime expiresAt;

    public WpsUserToken(String accessToken, OffsetDateTime expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
