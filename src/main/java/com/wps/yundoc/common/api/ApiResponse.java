package com.wps.yundoc.common.api;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;
    private final String requestId;
    private final Pagination pagination;

    private ApiResponse(boolean success, T data, ErrorResponse error, String requestId, Pagination pagination) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.requestId = requestId;
        this.pagination = pagination;
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, data, null, requestId, null);
    }

    public static <T> ApiResponse<T> success(T data, String requestId, Pagination pagination) {
        return new ApiResponse<>(true, data, null, requestId, pagination);
    }

    public static <T> ApiResponse<T> failure(ErrorResponse error, String requestId) {
        return new ApiResponse<>(false, null, error, requestId, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }

    public String getRequestId() {
        return requestId;
    }

    public Pagination getPagination() {
        return pagination;
    }
}
