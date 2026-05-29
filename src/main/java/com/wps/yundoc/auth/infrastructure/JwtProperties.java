package com.wps.yundoc.auth.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.Duration;

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

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
