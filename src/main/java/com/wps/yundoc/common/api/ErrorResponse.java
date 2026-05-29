package com.wps.yundoc.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String upstreamCategory;
    private final Map<String, Object> details;

    private ErrorResponse(String code, String message, String upstreamCategory, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.upstreamCategory = upstreamCategory;
        this.details = details;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, null);
    }

    public static ErrorResponse of(String code, String message, String upstreamCategory) {
        return new ErrorResponse(code, message, upstreamCategory, null);
    }

    public static ErrorResponse of(
            String code,
            String message,
            String upstreamCategory,
            Map<String, Object> details) {
        return new ErrorResponse(code, message, upstreamCategory, details);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getUpstreamCategory() {
        return upstreamCategory;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
