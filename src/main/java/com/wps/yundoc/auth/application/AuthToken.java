package com.wps.yundoc.auth.application;

import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;

import java.util.List;

/**
 * AuthToken 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class AuthToken {

    private final String accessToken;
    private final long expiresIn;
    private final BusinessSystemPrincipal principal;
    private final List<String> apiPermissions;

    public AuthToken(
            String accessToken,
            long expiresIn,
            BusinessSystemPrincipal principal,
            List<String> apiPermissions) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.principal = principal;
        this.apiPermissions = apiPermissions;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public BusinessSystemPrincipal getPrincipal() {
        return principal;
    }

    public List<String> getApiPermissions() {
        return apiPermissions;
    }
}
