package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * WpsClientSupport component.
 *
 * @author WPS
 */
final class WpsClientSupport {

    private static final int SUCCESS_CODE = 0;
    private static final String HTTPS_SCHEME = "https";

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
        requireSecureBaseUrl(properties);
        return builder
                .requestFactory(NoRedirectSimpleClientHttpRequestFactory::new)
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }

    static void requireSecureBaseUrl(WpsClientProperties properties) {
        URI baseUri = baseUri(properties.getBaseUrl());
        if (!HTTPS_SCHEME.equalsIgnoreCase(baseUri.getScheme()) || baseUri.getHost() == null) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "Invalid WPS base url");
        }
        if (baseUri.getUserInfo() != null || baseUri.getQuery() != null || baseUri.getFragment() != null) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "Invalid WPS base url");
        }
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

    private static URI baseUri(String baseUrl) {
        try {
            return new URI(baseUrl);
        } catch (URISyntaxException ex) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "Invalid WPS base url", ex);
        }
    }

    private static class NoRedirectSimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false);
        }
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
