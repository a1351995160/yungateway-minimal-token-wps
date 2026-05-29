package com.wps.yundoc.credential.domain;

public class WpsOAuthCallbackResult {

    private final String userId;
    private final String status;

    public WpsOAuthCallbackResult(String userId) {
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
