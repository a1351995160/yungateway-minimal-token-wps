package com.wps.yundoc.auth.api;

import com.wps.yundoc.auth.application.AuthToken;

import java.util.List;

/**
 * TokenResponse component.
 *
 * @author WPS
 */
public class TokenResponse {

    private static final String TOKEN_TYPE = "Bearer";

    private final String accessToken;
    private final long expiresIn;
    private final List<String> apiPermissions;
    private final String identityType;
    private final String userId;

    public TokenResponse(AuthToken token) {
        this.accessToken = token.getAccessToken();
        this.expiresIn = token.getExpiresIn();
        this.apiPermissions = token.getApiPermissions();
        this.identityType = token.getPrincipal().getIdentityType().name();
        this.userId = token.getPrincipal().getUserId();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return TOKEN_TYPE;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public List<String> getApiPermissions() {
        return apiPermissions;
    }

    public String getIdentityType() {
        return identityType;
    }

    public String getUserId() {
        return userId;
    }
}
