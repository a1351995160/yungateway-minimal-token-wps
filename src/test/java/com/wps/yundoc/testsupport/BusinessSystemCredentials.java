package com.wps.yundoc.testsupport;

import java.security.PrivateKey;

public class BusinessSystemCredentials {

    private final String businessSystemId;
    private final String clientId;
    private final String clientSecret;
    private final PrivateKey userAssertionPrivateKey;

    public BusinessSystemCredentials(
            String businessSystemId,
            String clientId,
            String clientSecret,
            PrivateKey userAssertionPrivateKey) {
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userAssertionPrivateKey = userAssertionPrivateKey;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public PrivateKey getUserAssertionPrivateKey() {
        return userAssertionPrivateKey;
    }
}
