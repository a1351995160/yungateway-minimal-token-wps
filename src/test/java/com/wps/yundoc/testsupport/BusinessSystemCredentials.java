package com.wps.yundoc.testsupport;

public class BusinessSystemCredentials {

    private final String businessSystemId;
    private final String clientId;
    private final String clientSecret;
    private final String userAssertionSigningKey;

    public BusinessSystemCredentials(
            String businessSystemId,
            String clientId,
            String clientSecret,
            String userAssertionSigningKey) {
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userAssertionSigningKey = userAssertionSigningKey;
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

    public String getUserAssertionSigningKey() {
        return userAssertionSigningKey;
    }
}
