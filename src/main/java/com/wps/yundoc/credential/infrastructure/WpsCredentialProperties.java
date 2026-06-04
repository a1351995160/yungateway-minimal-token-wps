package com.wps.yundoc.credential.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * WpsCredentialProperties 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@ConfigurationProperties(prefix = "yundoc.wps-credential")
public class WpsCredentialProperties {

    private Duration refreshSkew = Duration.ofMinutes(5);
    private int maxUserTokenCount = 10000;

    public Duration getRefreshSkew() {
        return refreshSkew;
    }

    public void setRefreshSkew(Duration refreshSkew) {
        this.refreshSkew = refreshSkew;
    }

    public int getMaxUserTokenCount() {
        return maxUserTokenCount;
    }

    public void setMaxUserTokenCount(int maxUserTokenCount) {
        this.maxUserTokenCount = maxUserTokenCount;
    }
}
