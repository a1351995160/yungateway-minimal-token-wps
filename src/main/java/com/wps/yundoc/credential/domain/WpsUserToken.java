package com.wps.yundoc.credential.domain;

import java.time.OffsetDateTime;

/**
 * WpsUserToken component.
 *
 * @author WPS
 */
public class WpsUserToken {

    private final String accessToken;
    private final OffsetDateTime expiresAt;
    private final String refreshToken;
    private final OffsetDateTime refreshExpiresAt;
    private final String tokenType;

    public WpsUserToken(String accessToken, OffsetDateTime expiresAt) {
        this(accessToken, expiresAt, null, null, null);
    }

    public WpsUserToken(
            String accessToken,
            OffsetDateTime expiresAt,
            String refreshToken,
            OffsetDateTime refreshExpiresAt,
            String tokenType) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.refreshToken = refreshToken;
        this.refreshExpiresAt = refreshExpiresAt;
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public OffsetDateTime getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public String getTokenType() {
        return tokenType;
    }
}
