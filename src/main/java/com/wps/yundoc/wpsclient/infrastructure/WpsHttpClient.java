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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    private static final long PREVIEW_EXPIRY_SKEW_SECONDS = 30L;

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;
    private final WpsRequestSigner signer;

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
        this.signer = WpsRequestSigner.fromProperties(properties);
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
        WpsOauthTokenResponse response = WpsClientSupport.executeWithRetry(
                properties,
                this::executeAppTokenOnce);
        return toAppToken(response);
    }

    private WpsPreviewResponse executePreviewOnce(WpsPreviewRequest request) {
        HttpEntity<byte[]> entity = previewEntity(request);
        return restTemplate.exchange(
                previewUrl(),
                HttpMethod.POST,
                entity,
                WpsPreviewResponse.class).getBody();
    }

    private WpsOauthTokenResponse executeAppTokenOnce() {
        return restTemplate.exchange(
                tokenUrl(),
                HttpMethod.POST,
                appTokenEntity(),
                WpsOauthTokenResponse.class).getBody();
    }

    private WpsPreviewLink toPreviewLink(WpsPreviewResponse response, WpsPreviewRequest request) {
        PreviewData data = requirePreviewData(response);
        OffsetDateTime expireAt = parseExpireAt(data.getExpireAt());
        validatePreviewUrl(data.getPreviewUrl());
        validatePreviewExpiry(expireAt, request);
        return new WpsPreviewLink(data.getPreviewUrl(), expireAt);
    }

    private WpsAppToken toAppToken(WpsOauthTokenResponse response) {
        WpsOauthTokenResponse data = WpsClientSupport.requireData(response);
        WpsClientSupport.requireText(data.getAccessToken());
        return new WpsAppToken(data.getAccessToken(), expiresAt(data.getExpiresIn()));
    }

    private PreviewData requirePreviewData(WpsPreviewResponse response) {
        PreviewData data = WpsClientSupport.requireSuccessData(response);
        WpsClientSupport.requireText(data.getPreviewUrl());
        return data;
    }

    private HttpEntity<byte[]> previewEntity(WpsPreviewRequest request) {
        PreviewPayload payload = new PreviewPayload(request.getFileId(), request.getExpireSeconds());
        byte[] body = WpsSignedRequestSupport.jsonBody(payload);
        HttpHeaders headers = WpsSignedRequestSupport.signedJsonHeaders(
                properties,
                signer,
                HttpMethod.POST.name(),
                previewUrl(),
                body);
        headers.setBearerAuth(request.getAccessToken());
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<MultiValueMap<String, String>> appTokenEntity() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.getAppId());
        body.add("client_secret", properties.getAppSecret());
        return new HttpEntity<>(body, formHeaders());
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private String previewUrl() {
        return properties.getBaseUrl() + properties.getPreviewPath();
    }

    private String tokenUrl() {
        return properties.getBaseUrl() + properties.getTokenPath();
    }

    private OffsetDateTime parseExpireAt(String expireAt) {
        WpsClientSupport.requireText(expireAt);
        try {
            return OffsetDateTime.parse(expireAt);
        } catch (DateTimeParseException ex) {
            throw WpsClientSupport.upstreamError(ex);
        }
    }

    private OffsetDateTime expiresAt(Long expiresIn) {
        if (expiresIn == null || expiresIn.longValue() <= 0L) {
            throw WpsClientSupport.upstreamError(null);
        }
        return OffsetDateTime.now().plusSeconds(expiresIn.longValue());
    }

    private void validatePreviewUrl(String previewUrl) {
        URI uri = uri(previewUrl);
        if (!isValidPreviewUri(uri)) {
            throw WpsClientSupport.upstreamError(null);
        }
    }

    private boolean isValidPreviewUri(URI uri) {
        return WpsClientSupport.isSecureHttpsUri(uri) && isAllowedPreviewUri(uri);
    }

    private boolean isAllowedPreviewUri(URI uri) {
        return uri.getUserInfo() == null && allowedPreviewHosts().contains(normalizeHost(uri.getHost()));
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
