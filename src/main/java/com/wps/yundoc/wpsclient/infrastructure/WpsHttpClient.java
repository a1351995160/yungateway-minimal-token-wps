package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class WpsHttpClient implements WpsPreviewClient, WpsAppTokenClient {

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;

    public WpsHttpClient(WpsClientProperties properties, RestTemplateBuilder builder) {
        this(properties, builder, restTemplate(properties, builder));
    }

    public WpsHttpClient(
            WpsClientProperties properties,
            RestTemplateBuilder builder,
            RestTemplate restTemplate) {
        Objects.requireNonNull(builder, "builder");
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public WpsPreviewLink createPreview(WpsPreviewRequest request) {
        WpsPreviewResponse response = executeWithRetry(() -> executePreviewOnce(request));
        return toPreviewLink(response);
    }

    @Override
    public WpsAppToken issueAppToken() {
        WpsAppTokenResponse response = executeWithRetry(this::executeAppTokenOnce);
        return toAppToken(response);
    }

    private <T> T executeWithRetry(WpsCall<T> call) {
        int maxAttempts = maxAttempts();
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

    private WpsPreviewResponse executePreviewOnce(WpsPreviewRequest request) {
        HttpEntity<PreviewPayload> entity = previewEntity(request);
        return restTemplate.exchange(
                previewUrl(),
                HttpMethod.POST,
                entity,
                WpsPreviewResponse.class).getBody();
    }

    private WpsAppTokenResponse executeAppTokenOnce() {
        return restTemplate.exchange(
                tokenUrl(),
                HttpMethod.POST,
                appTokenEntity(),
                WpsAppTokenResponse.class).getBody();
    }

    private WpsPreviewLink toPreviewLink(WpsPreviewResponse response) {
        PreviewData data = requirePreviewData(response);
        OffsetDateTime expireAt = parseExpireAt(data.getExpireAt());
        return new WpsPreviewLink(data.getPreviewUrl(), expireAt);
    }

    private WpsAppToken toAppToken(WpsAppTokenResponse response) {
        AppTokenData data = requireAppTokenData(response);
        OffsetDateTime expireAt = parseExpireAt(data.getExpireAt());
        return new WpsAppToken(data.getAccessToken(), expireAt);
    }

    private PreviewData requirePreviewData(WpsPreviewResponse response) {
        if (!hasSuccessEnvelope(response)) {
            throw upstreamError(null);
        }
        PreviewData data = response.getData();
        if (data == null) {
            throw upstreamError(null);
        }
        if (!hasText(data.getPreviewUrl())) {
            throw upstreamError(null);
        }
        return data;
    }

    private AppTokenData requireAppTokenData(WpsAppTokenResponse response) {
        if (!hasSuccessEnvelope(response)) {
            throw upstreamError(null);
        }
        AppTokenData data = response.getData();
        if (data == null) {
            throw upstreamError(null);
        }
        if (!hasText(data.getAccessToken())) {
            throw upstreamError(null);
        }
        return data;
    }

    private boolean hasSuccessEnvelope(WpsEnvelope<?> response) {
        if (response == null) {
            return false;
        }
        if (response.getCode() == null) {
            return false;
        }
        return response.getCode().intValue() == 0;
    }

    private HttpEntity<PreviewPayload> previewEntity(WpsPreviewRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(request.getAccessToken());
        PreviewPayload payload = new PreviewPayload(request.getFileId(), request.getExpireSeconds());
        return new HttpEntity<>(payload, headers);
    }

    private HttpEntity<AppTokenPayload> appTokenEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        AppTokenPayload payload = new AppTokenPayload(properties.getAppId(), properties.getAppSecret());
        return new HttpEntity<>(payload, headers);
    }

    private void handleRetry(int attempt, int maxAttempts, ResourceAccessException ex) {
        if (attempt < maxAttempts) {
            return;
        }
        throw upstreamError(ex);
    }

    private void handleHttpRetry(int attempt, int maxAttempts, HttpStatusCodeException ex) {
        if (canRetryHttp(attempt, maxAttempts, ex)) {
            return;
        }
        throw upstreamError(ex);
    }

    private boolean canRetryHttp(int attempt, int maxAttempts, HttpStatusCodeException ex) {
        if (attempt >= maxAttempts) {
            return false;
        }
        return isRetryableStatus(ex);
    }

    private boolean isRetryableStatus(HttpStatusCodeException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return true;
        }
        return ex.getRawStatusCode() == 429;
    }

    private int maxAttempts() {
        return Math.max(0, properties.getMaxRetries()) + 1;
    }

    private String previewUrl() {
        return properties.getBaseUrl() + properties.getPreviewPath();
    }

    private String tokenUrl() {
        return properties.getBaseUrl() + properties.getTokenPath();
    }

    private OffsetDateTime parseExpireAt(String expireAt) {
        if (!hasText(expireAt)) {
            throw upstreamError(null);
        }
        try {
            return OffsetDateTime.parse(expireAt);
        } catch (DateTimeParseException ex) {
            throw upstreamError(ex);
        }
    }

    private boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        return !value.trim().isEmpty();
    }

    private YundocException upstreamError(Throwable cause) {
        return new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "WPS upstream error", cause);
    }

    private static RestTemplate restTemplate(WpsClientProperties properties, RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }

    private interface WpsCall<T> {

        T execute();
    }
}
