package com.wps.yundoc.common.error;

import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.api.ErrorResponse;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * GlobalExceptionHandler 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(YundocException.class)
    public ResponseEntity<ApiResponse<Void>> handleYundocException(YundocException exception) {
        YundocErrorCode code = exception.getErrorCode();
        String requestId = requestId();
        logYundocException(exception, requestId);
        ErrorResponse error = ErrorResponse.of(
                code.name(),
                exception.getMessage(),
                exception.getUpstreamCategory(),
                exception.getDetails());
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestPartException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        String requestId = requestId();
        LOGGER.warn("请求参数校验失败 请求ID={} 异常类型={}",
                requestId,
                exception.getClass().getSimpleName());
        ErrorResponse error = ErrorResponse.of(
                YundocErrorCode.VALIDATION_FAILED.name(),
                YundocErrorCode.VALIDATION_FAILED.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception exception) {
        String requestId = requestId();
        LOGGER.error("请求处理发生未捕获异常 请求ID={}", requestId, exception);
        ErrorResponse error = ErrorResponse.of(
                YundocErrorCode.INTERNAL_ERROR.name(),
                YundocErrorCode.INTERNAL_ERROR.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error, requestId));
    }

    private void logYundocException(YundocException exception, String requestId) {
        YundocErrorCode code = exception.getErrorCode();
        if (code == YundocErrorCode.WPS_UPSTREAM_ERROR || code == YundocErrorCode.INTERNAL_ERROR) {
            LOGGER.error("业务请求处理失败 请求ID={} 错误码={} 上游分类={}",
                    requestId,
                    code,
                    exception.getUpstreamCategory(),
                    exception);
            return;
        }
        LOGGER.warn("业务请求被拒绝 请求ID={} 错误码={} 上游分类={}",
                requestId,
                code,
                exception.getUpstreamCategory());
    }

    private String requestId() {
        return RequestContextHolder.currentRequestId().orElse("unknown");
    }
}
