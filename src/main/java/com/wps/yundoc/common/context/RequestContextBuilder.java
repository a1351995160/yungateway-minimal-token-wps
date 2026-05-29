package com.wps.yundoc.common.context;

public class RequestContextBuilder {

    private final String requestId;
    private String businessSystemId;
    private String clientId;
    private String jti;
    private Integer tokenVersion;
    private Integer permissionVersion;
    private String apiCode;
    private String userId;

    RequestContextBuilder(String requestId) {
        this.requestId = requestId;
    }

    public RequestContextBuilder businessSystemId(String value) {
        this.businessSystemId = value;
        return this;
    }

    public RequestContextBuilder clientId(String value) {
        this.clientId = value;
        return this;
    }

    public RequestContextBuilder jti(String value) {
        this.jti = value;
        return this;
    }

    public RequestContextBuilder tokenVersion(Integer value) {
        this.tokenVersion = value;
        return this;
    }

    public RequestContextBuilder permissionVersion(Integer value) {
        this.permissionVersion = value;
        return this;
    }

    public RequestContextBuilder apiCode(String value) {
        this.apiCode = value;
        return this;
    }

    public RequestContextBuilder userId(String value) {
        this.userId = value;
        return this;
    }

    public RequestContext build() {
        return new RequestContext(this);
    }

    String requestId() {
        return requestId;
    }

    String businessSystemId() {
        return businessSystemId;
    }

    String clientId() {
        return clientId;
    }

    String jti() {
        return jti;
    }

    Integer tokenVersion() {
        return tokenVersion;
    }

    Integer permissionVersion() {
        return permissionVersion;
    }

    String apiCode() {
        return apiCode;
    }

    String userId() {
        return userId;
    }
}
