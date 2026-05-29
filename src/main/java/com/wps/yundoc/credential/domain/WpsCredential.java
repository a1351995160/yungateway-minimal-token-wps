package com.wps.yundoc.credential.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * WpsCredential component.
 *
 * @author WPS
 */
public class WpsCredential {

    private final String accessToken;
    private final OffsetDateTime expiresAt;

    public WpsCredential(String accessToken, OffsetDateTime expiresAt) {
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
        if (!(other instanceof WpsCredential)) {
            return false;
        }
        WpsCredential that = (WpsCredential) other;
        return Objects.equals(accessToken, that.accessToken)
                && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, expiresAt);
    }
}
