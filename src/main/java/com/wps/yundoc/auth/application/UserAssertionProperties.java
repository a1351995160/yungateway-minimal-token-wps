package com.wps.yundoc.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * UserAssertionProperties 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@ConfigurationProperties(prefix = "yundoc.user-assertion")
public class UserAssertionProperties {

    private Duration maxClockSkew = Duration.ofMinutes(5);
    private int maxTrackedNonces = 10000;

    public Duration getMaxClockSkew() {
        return maxClockSkew;
    }

    public void setMaxClockSkew(Duration maxClockSkew) {
        this.maxClockSkew = maxClockSkew;
    }

    public int getMaxTrackedNonces() {
        return maxTrackedNonces;
    }

    public void setMaxTrackedNonces(int maxTrackedNonces) {
        this.maxTrackedNonces = maxTrackedNonces;
    }
}
