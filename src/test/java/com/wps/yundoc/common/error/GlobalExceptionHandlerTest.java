package com.wps.yundoc.common.error;

import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void yundocExceptionUsesStableEnvelope() {
        RequestContextHolder.set(RequestContext.builder("req-101").build());

        ResponseEntity<ApiResponse<Void>> response = handler.handleYundocException(
                new YundocException(YundocErrorCode.API_PERMISSION_DENIED, "denied"));

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getRequestId()).isEqualTo("req-101");
        assertThat(response.getBody().getError().getCode()).isEqualTo("API_PERMISSION_DENIED");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("denied");
    }

    @Test
    void unhandledExceptionDoesNotLeakOriginalMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnhandledException(
                new IllegalStateException("access_token=secret"));

        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getError().getMessage()).doesNotContain("secret");
    }
}
