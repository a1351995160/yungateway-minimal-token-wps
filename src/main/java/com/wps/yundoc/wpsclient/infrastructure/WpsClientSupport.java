package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
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
    private static final String INVALID_BASE_URL_MESSAGE = "Invalid WPS base url";

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

    static <T> T requireSuccessData(WpsEnvelope<T> response) {
        if (!isSuccessEnvelope(response)) {
            throw upstreamError(null);
        }
        return requireData(response.getData());
    }

    static <T> T requireData(T data) {
        if (data == null) {
            throw upstreamError(null);
        }
        return data;
    }

    static String requireText(String value) {
        if (!Texts.hasText(value)) {
            throw upstreamError(null);
        }
        return value;
    }

    static <T> T executeWithRetry(WpsClientProperties properties, WpsCall<T> call) {
        int maxAttempts = maxAttempts(properties);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.execute();
            } catch (RestClientException ex) {
                handleRetry(attempt, maxAttempts, ex);
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
        if (!isSecureBaseUri(baseUri)) {
            throw invalidBaseUrl();
        }
    }

    static boolean isSecureHttpsUri(URI uri) {
        return HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    }

    static boolean hasNoAuthorityExtras(URI uri) {
        return uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null;
    }

    private static void handleRetry(int attempt, int maxAttempts, RestClientException ex) {
        if (canRetry(attempt, maxAttempts, ex)) {
            return;
        }
        throw upstreamError(ex);
    }

    private static boolean canRetry(int attempt, int maxAttempts, RestClientException ex) {
        if (attempt >= maxAttempts) {
            return false;
        }
        return isRetryableException(ex);
    }

    private static boolean isRetryableException(RestClientException ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        return ex instanceof HttpStatusCodeException && isRetryableStatus((HttpStatusCodeException) ex);
    }

    private static boolean isRetryableStatus(HttpStatusCodeException ex) {
        return ex.getStatusCode().is5xxServerError() || ex.getRawStatusCode() == 429;
    }

    private static int maxAttempts(WpsClientProperties properties) {
        return Math.max(0, properties.getMaxRetries()) + 1;
    }

    private static URI baseUri(String baseUrl) {
        try {
            return new URI(baseUrl);
        } catch (URISyntaxException ex) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, INVALID_BASE_URL_MESSAGE, ex);
        }
    }

    private static boolean isSecureBaseUri(URI baseUri) {
        return isSecureHttpsUri(baseUri) && hasNoAuthorityExtras(baseUri);
    }

    private static YundocException invalidBaseUrl() {
        return new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, INVALID_BASE_URL_MESSAGE);
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
