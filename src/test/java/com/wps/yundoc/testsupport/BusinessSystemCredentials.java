package com.wps.yundoc.testsupport;

public class BusinessSystemCredentials {

    private final String businessSystemId;
    private final String clientId;
    private final String clientSecret;

    public BusinessSystemCredentials(String businessSystemId, String clientId, String clientSecret) {
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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
}
