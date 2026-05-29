package com.wps.yundoc.wpsclient.infrastructure;

/**
 * AppTokenPayload component.
 *
 * @author WPS
 */
class AppTokenPayload {

    private final String appId;
    private final String appSecret;

    AppTokenPayload(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }
}
