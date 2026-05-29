package com.wps.yundoc.credential.domain;

/**
 * WpsOauthCallbackResult component.
 *
 * @author WPS
 */
public class WpsOauthCallbackResult {

    private final String userId;
    private final String status;

    public WpsOauthCallbackResult(String userId) {
        this.userId = userId;
        this.status = "AUTHORIZED";
    }

    public String getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }
}
