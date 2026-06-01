package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
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
 * WpsAuthorizationHttpClient component.
 *
 * @author WPS
 */
public class WpsAuthorizationHttpClient implements WpsAuthorizationClient {

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
        WpsOauthTokenResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(authorizationCodeBody(code)));
        return toUserToken(response);
    }

    @Override
    public WpsUserToken refreshToken(String refreshToken) {
        WpsOauthTokenResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(refreshTokenBody(refreshToken)));
        return toUserToken(response);
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
}
