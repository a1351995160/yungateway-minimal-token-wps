package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
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
    private String signatureAlgorithm = YundocCryptoAlgorithms.HMAC_SM3;
    private boolean legacySignatureEnabled = true;

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

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public boolean isLegacySignatureEnabled() {
        return legacySignatureEnabled;
    }

    public void setLegacySignatureEnabled(boolean legacySignatureEnabled) {
        this.legacySignatureEnabled = legacySignatureEnabled;
    }
}
