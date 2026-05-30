package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
        WpsAppTokenResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(code));
        return toUserToken(response);
    }

    private WpsAppTokenResponse exchange(String code) {
        return restTemplate.exchange(
                userTokenUrl(),
                HttpMethod.POST,
                entity(code),
                WpsAppTokenResponse.class).getBody();
    }

    private WpsUserToken toUserToken(WpsAppTokenResponse response) {
        AppTokenData data = requireData(response);
        return new WpsUserToken(data.getAccessToken(), parseExpireAt(data.getExpireAt()));
    }

    private AppTokenData requireData(WpsAppTokenResponse response) {
        AppTokenData data = WpsClientSupport.requireSuccessData(response);
        WpsClientSupport.requireText(data.getAccessToken());
        WpsClientSupport.requireText(data.getExpireAt());
        return data;
    }

    private HttpEntity<OauthCodePayload> entity(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        OauthCodePayload payload = new OauthCodePayload(
                code,
                properties.getAppId(),
                properties.getAppSecret(),
                properties.getRedirectUri());
        return new HttpEntity<>(payload, headers);
    }

    private String baseAuthorizeUrl() {
        return properties.getBaseUrl() + properties.getAuthorizePath();
    }

    private String userTokenUrl() {
        return properties.getBaseUrl() + properties.getUserTokenPath();
    }

    private OffsetDateTime parseExpireAt(String expireAt) {
        try {
            return OffsetDateTime.parse(expireAt);
        } catch (DateTimeParseException ex) {
            throw WpsClientSupport.upstreamError(ex);
        }
    }
}
