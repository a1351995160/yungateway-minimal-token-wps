package com.wps.yundoc.credential.domain;

/**
 * WpsAuthorizationLink 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsAuthorizationLink {

    private final String authorizeUrl;
    private final long expiresIn;

    public WpsAuthorizationLink(String authorizeUrl, long expiresIn) {
        this.authorizeUrl = authorizeUrl;
        this.expiresIn = expiresIn;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
