package com.wps.yundoc.wpsclient.application;

import java.time.OffsetDateTime;
import java.util.Objects;

public class WpsAppToken {

    private final String accessToken;
    private final OffsetDateTime expiresAt;

    public WpsAppToken(String accessToken, OffsetDateTime expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WpsAppToken)) {
            return false;
        }
        WpsAppToken that = (WpsAppToken) other;
        return Objects.equals(accessToken, that.accessToken)
                && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, expiresAt);
    }
}
