package com.wps.yundoc.auth.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * TokenRequest 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class TokenRequest {

    @NotBlank
    @Size(max = 64)
    private String clientId;

    @NotBlank
    @Size(max = 128)
    private String clientSecret;

    @Size(max = 16)
    private String identityType;

    @Size(max = 128)
    private String userId;

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

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
