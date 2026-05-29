package com.wps.yundoc.common.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successResponseCarriesDataAndRequestId() {
        ApiResponse<String> response = ApiResponse.success("ok", "req-001");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("ok");
        assertThat(response.getError()).isNull();
        assertThat(response.getRequestId()).isEqualTo("req-001");
        assertThat(response.getPagination()).isNull();
    }

    @Test
    void failureResponseCarriesErrorAndRequestId() {
        ErrorResponse error = ErrorResponse.of("VALIDATION_FAILED", "invalid");

        ApiResponse<Void> response = ApiResponse.failure(error, "req-002");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getRequestId()).isEqualTo("req-002");
    }

    @Test
    void responseEnvelopeKeepsNullableFieldsStable() throws Exception {
        ApiResponse<String> response = ApiResponse.success("ok", "req-003");

        JsonNode jsonNode = objectMapper.valueToTree(response);

        assertThat(jsonNode.has("success")).isTrue();
        assertThat(jsonNode.has("data")).isTrue();
        assertThat(jsonNode.has("error")).isTrue();
        assertThat(jsonNode.get("error").isNull()).isTrue();
        assertThat(jsonNode.has("requestId")).isTrue();
        assertThat(jsonNode.has("pagination")).isTrue();
        assertThat(jsonNode.get("pagination").isNull()).isTrue();
    }
}
