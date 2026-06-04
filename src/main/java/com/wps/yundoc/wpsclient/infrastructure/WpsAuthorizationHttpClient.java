package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * WpsAuthorizationHttpClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsAuthorizationHttpClient implements WpsAuthorizationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsAuthorizationHttpClient.class);

    private static final int STATE_PREFIX_LENGTH = 8;
    private static final String EXCHANGE_USER_CODE_OPERATION = "用户授权码换取凭证";
    private static final String REFRESH_USER_TOKEN_OPERATION = "刷新用户凭证";

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;

    public WpsAuthorizationHttpClient(WpsClientProperties properties, RestTemplateBuilder builder) {
        this(properties, builder, WpsClientSupport.restTemplate(properties, builder));
    }

    public WpsAuthorizationHttpClient(
            WpsClientProperties properties,
            RestTemplateBuilder builder,
            RestTemplate restTemplate) {
        Objects.requireNonNull(builder, "builder");
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public String authorizeUrl(String state) {
        LOGGER.info("WPS用户授权地址已生成 请求路径={} 应用ID={} 状态前缀={}",
                properties.getAuthorizePath(),
                properties.getAppId(),
                statePrefix(state));
        return UriComponentsBuilder.fromHttpUrl(baseAuthorizeUrl())
                .queryParam("client_id", properties.getAppId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getOauthScope())
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public WpsUserToken exchangeCode(String code) {
        long startedAt = System.nanoTime();
        logTokenRequestStarted(EXCHANGE_USER_CODE_OPERATION);
        return executeTokenOperation(
                EXCHANGE_USER_CODE_OPERATION,
                startedAt,
                () -> exchange(authorizationCodeBody(code)));
    }

    @Override
    public WpsUserToken refreshToken(String refreshToken) {
        long startedAt = System.nanoTime();
        logTokenRequestStarted(REFRESH_USER_TOKEN_OPERATION);
        return executeTokenOperation(
                REFRESH_USER_TOKEN_OPERATION,
                startedAt,
                () -> exchange(refreshTokenBody(refreshToken)));
    }

    private WpsUserToken executeTokenOperation(
            String operation,
            long startedAt,
            WpsClientSupport.WpsCall<WpsOauthTokenResponse> call) {
        try {
            WpsOauthTokenResponse response = WpsClientSupport.executeWithRetry(properties, operation, call);
            WpsUserToken token = toUserToken(response);
            logTokenRequestCompleted(operation, token, startedAt);
            return token;
        } catch (RuntimeException ex) {
            logTokenRequestFailed(operation, startedAt, ex);
            throw ex;
        }
    }

    private void logTokenRequestStarted(String operation) {
        LOGGER.info("WPS请求开始 操作={} 请求方法=POST 请求路径={} 应用ID={}",
                operation,
                properties.getUserTokenPath(),
                properties.getAppId());
    }

    private void logTokenRequestCompleted(String operation, WpsUserToken token, long startedAt) {
        LOGGER.info("WPS请求完成 操作={} 应用ID={} 过期时间={} 耗时毫秒={}",
                operation,
                properties.getAppId(),
                token.getExpiresAt(),
                Long.valueOf(elapsedMillis(startedAt)));
    }

    private void logTokenRequestFailed(String operation, long startedAt, RuntimeException ex) {
        LOGGER.error("WPS请求失败 操作={} 应用ID={} 耗时毫秒={}",
                operation,
                properties.getAppId(),
                Long.valueOf(elapsedMillis(startedAt)),
                ex);
    }

    private WpsOauthTokenResponse exchange(MultiValueMap<String, String> body) {
        return restTemplate.exchange(
                userTokenUrl(),
                HttpMethod.POST,
                entity(body),
                WpsOauthTokenResponse.class).getBody();
    }

    private WpsUserToken toUserToken(WpsOauthTokenResponse response) {
        WpsOauthTokenResponse data = requireData(response);
        return new WpsUserToken(
                data.getAccessToken(),
                expiresAt(data.getExpiresIn()),
                data.getRefreshToken(),
                expiresAt(data.getRefreshExpiresIn()),
                data.getTokenType());
    }

    private WpsOauthTokenResponse requireData(WpsOauthTokenResponse response) {
        WpsOauthTokenResponse data = WpsClientSupport.requireData(response);
        WpsClientSupport.requireText(data.getAccessToken());
        WpsClientSupport.requireText(data.getRefreshToken());
        return data;
    }

    private HttpEntity<MultiValueMap<String, String>> entity(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(body, headers);
    }

    private MultiValueMap<String, String> authorizationCodeBody(String code) {
        MultiValueMap<String, String> body = baseTokenBody("authorization_code");
        body.add("code", code);
        body.add("redirect_uri", properties.getRedirectUri());
        return body;
    }

    private MultiValueMap<String, String> refreshTokenBody(String refreshToken) {
        MultiValueMap<String, String> body = baseTokenBody("refresh_token");
        body.add("refresh_token", refreshToken);
        return body;
    }

    private MultiValueMap<String, String> baseTokenBody(String grantType) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("client_id", properties.getAppId());
        body.add("client_secret", properties.getAppSecret());
        return body;
    }

    private String baseAuthorizeUrl() {
        return properties.getBaseUrl() + properties.getAuthorizePath();
    }

    private String userTokenUrl() {
        return properties.getBaseUrl() + properties.getUserTokenPath();
    }

    private OffsetDateTime expiresAt(Long expiresIn) {
        if (expiresIn == null || expiresIn.longValue() <= 0L) {
            throw WpsClientSupport.upstreamError(null);
        }
        return OffsetDateTime.now().plusSeconds(expiresIn.longValue());
    }

    private String statePrefix(String state) {
        if (state == null || state.length() <= STATE_PREFIX_LENGTH) {
            return state;
        }
        return state.substring(0, STATE_PREFIX_LENGTH);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
