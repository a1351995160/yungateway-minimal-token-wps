package com.wps.yundoc.common.error;

public enum YundocErrorCode {
    AUTH_REQUIRED(401, "Authentication is required"),
    TOKEN_INVALID(401, "Token is invalid"),
    BUSINESS_SYSTEM_DISABLED(403, "Business system is disabled"),
    API_PERMISSION_DENIED(403, "API permission denied"),
    USER_ID_REQUIRED(400, "User id is required"),
    REAUTH_REQUIRED(401, "WPS user authorization is required"),
    VALIDATION_FAILED(400, "Request validation failed"),
    WPS_UPSTREAM_ERROR(502, "WPS upstream error"),
    INTERNAL_ERROR(500, "Internal server error");

    private final int httpStatus;
    private final String defaultMessage;

    YundocErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
