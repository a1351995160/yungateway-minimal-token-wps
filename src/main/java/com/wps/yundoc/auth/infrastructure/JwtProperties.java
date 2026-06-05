package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.Duration;

/**
 * JwtProperties 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Validated
@ConfigurationProperties(prefix = "yundoc.jwt")
public class JwtProperties {

    @NotBlank
    @Size(max = 64)
    private String issuer;

    @NotBlank
    @Size(max = 64)
    private String audience;

    @NotBlank
    @Size(min = 32, max = 256)
    private String secret;

    @NotBlank
    @Size(max = 32)
    private String algorithm = YundocCryptoAlgorithms.JWT_HSM3;

    private boolean legacyValidationEnabled = true;

    private Duration ttl = Duration.ofMinutes(30);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isLegacyValidationEnabled() {
        return legacyValidationEnabled;
    }

    public void setLegacyValidationEnabled(boolean legacyValidationEnabled) {
        this.legacyValidationEnabled = legacyValidationEnabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
