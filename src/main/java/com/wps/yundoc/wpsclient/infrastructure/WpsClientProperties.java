package com.wps.yundoc.wpsclient.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "yundoc.wps-client")
public class WpsClientProperties {

    private String baseUrl = "";
    private String previewPath = "";
    private String tokenPath = "";
    private String fileListPath = "";
    private String authorizePath = "";
    private String userTokenPath = "";
    private String redirectUri = "";
    private String oAuthScope = "";
    private String appId = "";
    private String appSecret = "";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private int maxRetries = 1;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getTokenPath() {
        return tokenPath;
    }

    public void setTokenPath(String tokenPath) {
        this.tokenPath = tokenPath;
    }

    public String getFileListPath() {
        return fileListPath;
    }

    public void setFileListPath(String fileListPath) {
        this.fileListPath = fileListPath;
    }

    public String getAuthorizePath() {
        return authorizePath;
    }

    public void setAuthorizePath(String authorizePath) {
        this.authorizePath = authorizePath;
    }

    public String getUserTokenPath() {
        return userTokenPath;
    }

    public void setUserTokenPath(String userTokenPath) {
        this.userTokenPath = userTokenPath;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getOAuthScope() {
        return oAuthScope;
    }

    public void setOAuthScope(String oAuthScope) {
        this.oAuthScope = oAuthScope;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
