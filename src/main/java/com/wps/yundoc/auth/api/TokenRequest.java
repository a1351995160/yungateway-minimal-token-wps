package com.wps.yundoc.auth.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TokenRequest {

    @NotBlank
    @Size(max = 64)
    private String clientId;

    @NotBlank
    @Size(max = 128)
    private String clientSecret;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
