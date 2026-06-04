package com.wps.yundoc.wpsclient.infrastructure;

/**
 * AppTokenPayload 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
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
