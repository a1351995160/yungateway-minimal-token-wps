package com.wps.yundoc.wpsclient.infrastructure;

/**
 * OauthCodePayload 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class OauthCodePayload {

    private final String code;
    private final String appId;
    private final String appSecret;
    private final String redirectUri;

    public OauthCodePayload(String code, String appId, String appSecret, String redirectUri) {
        this.code = code;
        this.appId = appId;
        this.appSecret = appSecret;
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
