package com.wps.yundoc.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.api.ErrorResponse;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public AuthErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, YundocException exception)
            throws IOException {
        YundocErrorCode code = exception.getErrorCode();
        response.setStatus(code.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body(exception));
    }

    private ApiResponse<Void> body(YundocException exception) {
        ErrorResponse error = ErrorResponse.of(
                exception.getErrorCode().name(),
                exception.getMessage(),
                exception.getUpstreamCategory(),
                exception.getDetails());
        return ApiResponse.failure(error, requestId());
    }

    private String requestId() {
        return RequestContextHolder.currentRequestId().orElse("unknown");
    }
}
