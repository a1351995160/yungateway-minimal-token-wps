package com.wps.yundoc.auth.domain;

public class BusinessSystemPrincipal {

    private final String businessSystemId;
    private final String clientId;
    private final String jti;
    private final Integer tokenVersion;
    private final Integer permissionVersion;

    public BusinessSystemPrincipal(
            String businessSystemId,
            String clientId,
            String jti,
            Integer tokenVersion,
            Integer permissionVersion) {
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.jti = jti;
        this.tokenVersion = tokenVersion;
        this.permissionVersion = permissionVersion;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getJti() {
        return jti;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public Integer getPermissionVersion() {
        return permissionVersion;
    }
}
