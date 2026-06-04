package com.wps.yundoc.common.error;

/**
 * YundocErrorCode 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public enum YundocErrorCode {
    /**
     * 需要认证。
     */
    AUTH_REQUIRED(401, "Authentication is required"),
    /**
     * 令牌校验失败。
     */
    TOKEN_INVALID(401, "Token is invalid"),
    /**
     * 业务系统已禁用。
     */
    BUSINESS_SYSTEM_DISABLED(403, "Business system is disabled"),
    /**
     * API 权限被拒绝。
     */
    API_PERMISSION_DENIED(403, "API permission denied"),
    /**
     * 用户 ID 必填。
     */
    USER_ID_REQUIRED(400, "User id is required"),
    /**
     * 需要 WPS 用户授权。
     */
    REAUTH_REQUIRED(401, "WPS user authorization is required"),
    /**
     * 用户断言签名校验失败。
     */
    USER_ASSERTION_INVALID(401, "User assertion is invalid"),
    /**
     * 请求参数校验失败。
     */
    VALIDATION_FAILED(400, "Request validation failed"),
    /**
     * 认证尝试次数过多。
     */
    RATE_LIMIT_EXCEEDED(429, "Too many authentication attempts"),
    /**
     * WPS 上游请求失败。
     */
    WPS_UPSTREAM_ERROR(502, "WPS upstream error"),
    /**
     * 服务内部错误。
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
