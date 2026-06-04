package com.wps.yundoc.credential.api;

import com.wps.yundoc.credential.domain.WpsAuthorizationLink;

/**
 * WpsAuthorizationLinkResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsAuthorizationLinkResponse {

    private final String authorizeUrl;
    private final long expiresIn;

    public WpsAuthorizationLinkResponse(WpsAuthorizationLink link) {
        this.authorizeUrl = link.getAuthorizeUrl();
        this.expiresIn = link.getExpiresIn();
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
