package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * WpsClientSupport component.
 *
 * @author WPS
 */
final class WpsClientSupport {

    private static final int SUCCESS_CODE = 0;

    private WpsClientSupport() {
    }

    static boolean isSuccessEnvelope(WpsEnvelope<?> response) {
        if (response == null) {
            return false;
        }
        if (response.getCode() == null) {
            return false;
        }
        return response.getCode().intValue() == SUCCESS_CODE;
    }

    static YundocException upstreamError(Throwable cause) {
        return new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "WPS upstream error", cause);
    }

    static <T> T executeWithRetry(WpsClientProperties properties, WpsCall<T> call) {
        int maxAttempts = maxAttempts(properties);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.execute();
            } catch (ResourceAccessException ex) {
                handleRetry(attempt, maxAttempts, ex);
            } catch (HttpStatusCodeException ex) {
                handleHttpRetry(attempt, maxAttempts, ex);
            } catch (RestClientException ex) {
                throw upstreamError(ex);
            }
        }
        throw upstreamError(null);
    }

    static RestTemplate restTemplate(WpsClientProperties properties, RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }

    private static void handleRetry(int attempt, int maxAttempts, ResourceAccessException ex) {
        if (attempt < maxAttempts) {
            return;
        }
        throw upstreamError(ex);
    }

    private static void handleHttpRetry(int attempt, int maxAttempts, HttpStatusCodeException ex) {
        if (canRetryHttp(attempt, maxAttempts, ex)) {
            return;
        }
        throw upstreamError(ex);
    }

    private static boolean canRetryHttp(int attempt, int maxAttempts, HttpStatusCodeException ex) {
        if (attempt >= maxAttempts) {
            return false;
        }
        return isRetryableStatus(ex);
    }

    private static boolean isRetryableStatus(HttpStatusCodeException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return true;
        }
        return ex.getRawStatusCode() == 429;
    }

    private static int maxAttempts(WpsClientProperties properties) {
        return Math.max(0, properties.getMaxRetries()) + 1;
    }

    interface WpsCall<T> {

        /**
         * Executes a single WPS request attempt.
         *
         * @return response from WPS
         */
        T execute();
    }
}
