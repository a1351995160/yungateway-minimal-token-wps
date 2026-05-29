package com.wps.yundoc.common.context;

/**
 * RequestContext component.
 *
 * @author WPS
 */
public class RequestContext {

    private final String requestId;
    private final String businessSystemId;
    private final String clientId;
    private final String jti;
    private final Integer tokenVersion;
    private final Integer permissionVersion;
    private final String apiCode;
    private final String userId;

    RequestContext(RequestContextBuilder builder) {
        this.requestId = builder.requestId();
        this.businessSystemId = builder.businessSystemId();
        this.clientId = builder.clientId();
        this.jti = builder.jti();
        this.tokenVersion = builder.tokenVersion();
        this.permissionVersion = builder.permissionVersion();
        this.apiCode = builder.apiCode();
        this.userId = builder.userId();
    }

    public static RequestContextBuilder builder(String requestId) {
        return new RequestContextBuilder(requestId);
    }

    public String getRequestId() {
        return requestId;
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

    public String getApiCode() {
        return apiCode;
    }

    public String getUserId() {
        return userId;
    }
}
