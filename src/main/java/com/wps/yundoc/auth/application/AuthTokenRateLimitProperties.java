package com.wps.yundoc.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AuthTokenRateLimitProperties component.
 *
 * @author WPS
 */
@ConfigurationProperties(prefix = "yundoc.auth-token-rate-limit")
public class AuthTokenRateLimitProperties {

    private boolean enabled = true;
    private int maxFailuresPerClient = 5;
    private int maxFailuresPerRemoteAddress = 20;
    private int maxTrackedKeys = 10000;
    private Duration window = Duration.ofMinutes(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxFailuresPerClient() {
        return maxFailuresPerClient;
    }

    public void setMaxFailuresPerClient(int maxFailuresPerClient) {
        this.maxFailuresPerClient = maxFailuresPerClient;
    }

    public int getMaxFailuresPerRemoteAddress() {
        return maxFailuresPerRemoteAddress;
    }

    public void setMaxFailuresPerRemoteAddress(int maxFailuresPerRemoteAddress) {
        this.maxFailuresPerRemoteAddress = maxFailuresPerRemoteAddress;
    }

    public int getMaxTrackedKeys() {
        return maxTrackedKeys;
    }

    public void setMaxTrackedKeys(int maxTrackedKeys) {
        this.maxTrackedKeys = maxTrackedKeys;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
