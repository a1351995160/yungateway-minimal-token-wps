package com.wps.yundoc.wpsclient.infrastructure;

public class OAuthCodePayload {

    private final String code;
    private final String appId;
    private final String appSecret;
    private final String redirectUri;

    public OAuthCodePayload(String code, String appId, String appSecret, String redirectUri) {
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
