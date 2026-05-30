package com.wps.yundoc.common.error;

/**
 * YundocErrorCode component.
 *
 * @author WPS
 */
public enum YundocErrorCode {
    /**
     * Authentication is required.
     */
    AUTH_REQUIRED(401, "Authentication is required"),
    /**
     * Token validation failed.
     */
    TOKEN_INVALID(401, "Token is invalid"),
    /**
     * Business system is disabled.
     */
    BUSINESS_SYSTEM_DISABLED(403, "Business system is disabled"),
    /**
     * API permission was denied.
     */
    API_PERMISSION_DENIED(403, "API permission denied"),
    /**
     * User id is required.
     */
    USER_ID_REQUIRED(400, "User id is required"),
    /**
     * WPS user authorization is required.
     */
    REAUTH_REQUIRED(401, "WPS user authorization is required"),
    /**
     * Request validation failed.
     */
    VALIDATION_FAILED(400, "Request validation failed"),
    /**
     * Too many authentication attempts were made.
     */
    RATE_LIMIT_EXCEEDED(429, "Too many authentication attempts"),
    /**
     * WPS upstream request failed.
     */
    WPS_UPSTREAM_ERROR(502, "WPS upstream error"),
    /**
     * Internal server error.
     */
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
