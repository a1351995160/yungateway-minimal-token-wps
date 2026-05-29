package com.wps.yundoc.common.error;

import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.api.ErrorResponse;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * GlobalExceptionHandler component.
 *
 * @author WPS
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(YundocException.class)
    public ResponseEntity<ApiResponse<Void>> handleYundocException(YundocException exception) {
        YundocErrorCode code = exception.getErrorCode();
        ErrorResponse error = ErrorResponse.of(
                code.name(),
                exception.getMessage(),
                exception.getUpstreamCategory(),
                exception.getDetails());
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.failure(error, requestId()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        ErrorResponse error = ErrorResponse.of(
                YundocErrorCode.VALIDATION_FAILED.name(),
                YundocErrorCode.VALIDATION_FAILED.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error, requestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception exception) {
        ErrorResponse error = ErrorResponse.of(
                YundocErrorCode.INTERNAL_ERROR.name(),
                YundocErrorCode.INTERNAL_ERROR.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error, requestId()));
    }

    private String requestId() {
        return RequestContextHolder.currentRequestId().orElse("unknown");
    }
}
