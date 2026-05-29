package com.wps.yundoc.auth.api;

import com.wps.yundoc.auth.application.AuthToken;

import java.util.List;

public class TokenResponse {

    private static final String TOKEN_TYPE = "Bearer";

    private final String accessToken;
    private final long expiresIn;
    private final List<String> apiPermissions;

    public TokenResponse(AuthToken token) {
        this.accessToken = token.getAccessToken();
        this.expiresIn = token.getExpiresIn();
        this.apiPermissions = token.getApiPermissions();
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
}
