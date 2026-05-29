package com.wps.yundoc.common.error;

/**
 * YundocException component.
 *
 * @author WPS
 */
public class YundocException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final YundocErrorCode errorCode;
    private final String upstreamCategory;
    private final transient java.util.Map<String, Object> details;

    public YundocException(YundocErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null, null);
    }

    public YundocException(YundocErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public YundocException(YundocErrorCode errorCode, String message, String upstreamCategory) {
        this(errorCode, message, upstreamCategory, null);
    }

    public YundocException(YundocErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public YundocException(YundocErrorCode errorCode, java.util.Map<String, Object> details) {
        this(errorCode, errorCode.getDefaultMessage(), null, null, details);
    }

    private YundocException(YundocErrorCode errorCode, String message, String upstreamCategory, Throwable cause) {
        this(errorCode, message, upstreamCategory, cause, null);
    }

    private YundocException(
            YundocErrorCode errorCode,
            String message,
            String upstreamCategory,
            Throwable cause,
            java.util.Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.upstreamCategory = upstreamCategory;
        this.details = details;
    }

    public YundocErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUpstreamCategory() {
        return upstreamCategory;
    }

    public java.util.Map<String, Object> getDetails() {
        return details;
    }
}
