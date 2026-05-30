package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.util.Texts;
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
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * WpsHttpClient component.
 *
 * @author WPS
 */
public class WpsHttpClient implements WpsPreviewClient, WpsAppTokenClient {

    private static final String HTTPS_SCHEME = "https";
    private static final long PREVIEW_EXPIRY_SKEW_SECONDS = 30L;

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;

    public WpsHttpClient(WpsClientProperties properties, RestTemplateBuilder builder) {
        this(properties, builder, WpsClientSupport.restTemplate(properties, builder));
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
        WpsPreviewResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> executePreviewOnce(request));
        return toPreviewLink(response, request);
    }

    @Override
    public WpsAppToken issueAppToken() {
        WpsAppTokenResponse response = WpsClientSupport.executeWithRetry(
                properties,
                this::executeAppTokenOnce);
        return toAppToken(response);
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

    private WpsPreviewLink toPreviewLink(WpsPreviewResponse response, WpsPreviewRequest request) {
        PreviewData data = requirePreviewData(response);
        OffsetDateTime expireAt = parseExpireAt(data.getExpireAt());
        validatePreviewUrl(data.getPreviewUrl());
        validatePreviewExpiry(expireAt, request);
        return new WpsPreviewLink(data.getPreviewUrl(), expireAt);
    }

    private WpsAppToken toAppToken(WpsAppTokenResponse response) {
        AppTokenData data = requireAppTokenData(response);
        OffsetDateTime expireAt = parseExpireAt(data.getExpireAt());
        return new WpsAppToken(data.getAccessToken(), expireAt);
    }

    private PreviewData requirePreviewData(WpsPreviewResponse response) {
        if (!WpsClientSupport.isSuccessEnvelope(response)) {
            throw WpsClientSupport.upstreamError(null);
        }
        PreviewData data = response.getData();
        if (data == null) {
            throw WpsClientSupport.upstreamError(null);
        }
        if (!Texts.hasText(data.getPreviewUrl())) {
            throw WpsClientSupport.upstreamError(null);
        }
        return data;
    }

    private AppTokenData requireAppTokenData(WpsAppTokenResponse response) {
        if (!WpsClientSupport.isSuccessEnvelope(response)) {
            throw WpsClientSupport.upstreamError(null);
        }
        AppTokenData data = response.getData();
        if (data == null) {
            throw WpsClientSupport.upstreamError(null);
        }
        if (!Texts.hasText(data.getAccessToken())) {
            throw WpsClientSupport.upstreamError(null);
        }
        return data;
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

    private String previewUrl() {
        return properties.getBaseUrl() + properties.getPreviewPath();
    }

    private String tokenUrl() {
        return properties.getBaseUrl() + properties.getTokenPath();
    }

    private OffsetDateTime parseExpireAt(String expireAt) {
        if (!Texts.hasText(expireAt)) {
            throw WpsClientSupport.upstreamError(null);
        }
        try {
            return OffsetDateTime.parse(expireAt);
        } catch (DateTimeParseException ex) {
            throw WpsClientSupport.upstreamError(ex);
        }
    }

    private void validatePreviewUrl(String previewUrl) {
        URI uri = uri(previewUrl);
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw WpsClientSupport.upstreamError(null);
        }
        if (uri.getUserInfo() != null || !allowedPreviewHosts().contains(normalizeHost(uri.getHost()))) {
            throw WpsClientSupport.upstreamError(null);
        }
    }

    private List<String> allowedPreviewHosts() {
        List<String> configuredHosts = properties.getPreviewUrlAllowedHosts();
        if (configuredHosts != null && !configuredHosts.isEmpty()) {
            return normalizeHosts(configuredHosts);
        }
        return normalizeHosts(java.util.Collections.singletonList(uri(properties.getBaseUrl()).getHost()));
    }

    private List<String> normalizeHosts(List<String> hosts) {
        List<String> normalized = new ArrayList<>();
        for (String host : hosts) {
            if (Texts.hasText(host)) {
                normalized.add(normalizeHost(host));
            }
        }
        return normalized;
    }

    private String normalizeHost(String host) {
        return host.toLowerCase(Locale.ROOT);
    }

    private URI uri(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            throw WpsClientSupport.upstreamError(ex);
        }
    }

    private void validatePreviewExpiry(OffsetDateTime expireAt, WpsPreviewRequest request) {
        OffsetDateTime maxExpireAt = OffsetDateTime.now(expireAt.getOffset())
                .plusSeconds(request.getExpireSeconds())
                .plusSeconds(PREVIEW_EXPIRY_SKEW_SECONDS);
        if (expireAt.isAfter(maxExpireAt)) {
            throw WpsClientSupport.upstreamError(null);
        }
    }

}
