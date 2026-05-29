package com.wps.yundoc.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "yundoc.client-secret")
public class ClientSecretDigestProperties {

    @NotBlank
    @Size(min = 16, max = 256)
    private String pepper = "";

    @NotBlank
    @Size(max = 32)
    private String algorithm = "HMAC-SHA256";

    public String getPepper() {
        return pepper;
    }

    public void setPepper(String pepper) {
        this.pepper = pepper;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
