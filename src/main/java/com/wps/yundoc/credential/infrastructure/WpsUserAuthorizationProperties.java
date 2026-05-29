package com.wps.yundoc.credential.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * WpsUserAuthorizationProperties component.
 *
 * @author WPS
 */
@ConfigurationProperties(prefix = "yundoc.wps-user-authorization")
public class WpsUserAuthorizationProperties {

    private Duration stateTtl = Duration.ofMinutes(5);
    private int maxStateCount = 10000;

    public Duration getStateTtl() {
        return stateTtl;
    }

    public void setStateTtl(Duration stateTtl) {
        this.stateTtl = stateTtl;
    }

    public int getMaxStateCount() {
        return maxStateCount;
    }

    public void setMaxStateCount(int maxStateCount) {
        this.maxStateCount = maxStateCount;
    }
}
