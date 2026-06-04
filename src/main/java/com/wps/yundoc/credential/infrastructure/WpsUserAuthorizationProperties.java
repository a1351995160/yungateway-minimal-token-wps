package com.wps.yundoc.credential.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * WpsUserAuthorizationProperties 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@ConfigurationProperties(prefix = "yundoc.wps-user-authorization")
public class WpsUserAuthorizationProperties {

    private Duration stateTtl = Duration.ofMinutes(5);
    private Duration refreshSkew = Duration.ofMinutes(5);
    private int maxStateCount = 10000;

    public Duration getStateTtl() {
        return stateTtl;
    }

    public void setStateTtl(Duration stateTtl) {
        this.stateTtl = stateTtl;
    }

    public Duration getRefreshSkew() {
        return refreshSkew;
    }

    public void setRefreshSkew(Duration refreshSkew) {
        this.refreshSkew = refreshSkew;
    }

    public int getMaxStateCount() {
        return maxStateCount;
    }

    public void setMaxStateCount(int maxStateCount) {
        this.maxStateCount = maxStateCount;
    }
}
