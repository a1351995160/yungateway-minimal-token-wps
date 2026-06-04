package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * WpsHttpClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsHttpClient implements WpsPreviewClient, WpsAppTokenClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsHttpClient.class);

    private static final long PREVIEW_EXPIRY_SKEW_SECONDS = 30L;
    private static final String CREATE_PREVIEW_OPERATION = "创建预览链接";
    private static final String ISSUE_APP_TOKEN_OPERATION = "申请应用凭证";

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
        long startedAt = System.nanoTime();
        logPreviewRequestStarted(request);
        return executePreviewRequest(request, startedAt);
    }

    private WpsPreviewLink executePreviewRequest(WpsPreviewRequest request, long startedAt) {
        try {
            WpsPreviewResponse response = WpsClientSupport.executeWithRetry(
                    properties,
                    CREATE_PREVIEW_OPERATION,
                    () -> executePreviewOnce(request));
            WpsPreviewLink link = toPreviewLink(response, request);
            logPreviewRequestCompleted(request, link, startedAt);
            return link;
        } catch (RuntimeException ex) {
            logPreviewRequestFailed(request, startedAt, ex);
            throw ex;
        }
    }

    @Override
    public WpsAppToken issueAppToken() {
        long startedAt = System.nanoTime();
        logAppTokenRequestStarted();
        return executeAppTokenRequest(startedAt);
    }

    private WpsAppToken executeAppTokenRequest(long startedAt) {
        try {
            WpsOauthTokenResponse response = WpsClientSupport.executeWithRetry(
                    properties,
                    ISSUE_APP_TOKEN_OPERATION,
                    this::executeAppTokenOnce);
            WpsAppToken appToken = toAppToken(response);
            logAppTokenRequestCompleted(appToken, startedAt);
            return appToken;
        } catch (RuntimeException ex) {
            logAppTokenRequestFailed(startedAt, ex);
            throw ex;
        }
    }

    private void logPreviewRequestStarted(WpsPreviewRequest request) {
        LOGGER.info("WPS请求开始 操作={} 请求方法=POST 请求路径={} WPS文件ID={} 预览有效秒数={}",
                CREATE_PREVIEW_OPERATION,
                properties.getPreviewPath(),
                request.getFileId(),
                Integer.valueOf(request.getExpireSeconds()));
    }

    private void logPreviewRequestCompleted(WpsPreviewRequest request, WpsPreviewLink link, long startedAt) {
        LOGGER.info("WPS请求完成 操作={} WPS文件ID={} 过期时间={} 耗时毫秒={}",
                CREATE_PREVIEW_OPERATION,
                request.getFileId(),
                link.getExpireAt(),
                Long.valueOf(elapsedMillis(startedAt)));
    }

    private void logPreviewRequestFailed(WpsPreviewRequest request, long startedAt, RuntimeException ex) {
        LOGGER.error("WPS请求失败 操作={} WPS文件ID={} 耗时毫秒={}",
                CREATE_PREVIEW_OPERATION,
                request.getFileId(),
                Long.valueOf(elapsedMillis(startedAt)),
                ex);
    }

    private void logAppTokenRequestStarted() {
        LOGGER.info("WPS请求开始 操作={} 请求方法=POST 请求路径={} 应用ID={}",
                ISSUE_APP_TOKEN_OPERATION,
                properties.getTokenPath(),
                properties.getAppId());
    }

    private void logAppTokenRequestCompleted(WpsAppToken appToken, long startedAt) {
        LOGGER.info("WPS请求完成 操作={} 应用ID={} 过期时间={} 耗时毫秒={}",
                ISSUE_APP_TOKEN_OPERATION,
                properties.getAppId(),
                appToken.getExpiresAt(),
                Long.valueOf(elapsedMillis(startedAt)));
    }

    private void logAppTokenRequestFailed(long startedAt, RuntimeException ex) {
        LOGGER.error("WPS请求失败 操作={} 应用ID={} 耗时毫秒={}",
                ISSUE_APP_TOKEN_OPERATION,
                properties.getAppId(),
                Long.valueOf(elapsedMillis(startedAt)),
                ex);
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
            LOGGER.warn("拒绝WPS预览地址 主机={} 协议={} 是否包含用户信息={}",
                    uri.getHost(),
                    uri.getScheme(),
                    Boolean.valueOf(uri.getUserInfo() != null));
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
            LOGGER.warn("拒绝WPS预览过期时间 WPS文件ID={} 实际过期时间={} 最大允许过期时间={}",
                    request.getFileId(),
                    expireAt,
                    maxExpireAt);
            throw WpsClientSupport.upstreamError(null);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
