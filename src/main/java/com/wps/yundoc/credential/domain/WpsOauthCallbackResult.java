package com.wps.yundoc.credential.domain;

/**
 * WpsOauthCallbackResult 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
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
