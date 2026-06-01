package com.wps.yundoc.credential.domain;

/**
 * WpsAuthorizationLink component.
 *
 * @author WPS
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
