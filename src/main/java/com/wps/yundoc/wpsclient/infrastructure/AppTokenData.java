package com.wps.yundoc.wpsclient.infrastructure;

/**
 * AppTokenData 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class AppTokenData {

    private String accessToken;
    private String expireAt;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        this.expireAt = expireAt;
    }
}
