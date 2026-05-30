package com.wps.yundoc.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * UserAssertionProperties component.
 *
 * @author WPS
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
