package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class WpsAuthorizationHttpClient implements WpsAuthorizationClient {

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;

    public WpsAuthorizationHttpClient(WpsClientProperties properties, RestTemplateBuilder builder) {
        this(properties, builder, restTemplate(properties, builder));
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
                .queryParam("scope", properties.getOAuthScope())
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public WpsUserToken exchangeCode(String code) {
        try {
            WpsAppTokenResponse response = exchange(code);
            return toUserToken(response);
        } catch (RestClientException ex) {
            throw upstreamError(ex);
        }
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
        if (!hasSuccessEnvelope(response)) {
            throw upstreamError(null);
        }
        if (response.getData() == null) {
            throw upstreamError(null);
        }
        if (!hasText(response.getData().getAccessToken())) {
            throw upstreamError(null);
        }
        if (!hasText(response.getData().getExpireAt())) {
            throw upstreamError(null);
        }
        return response.getData();
    }

    private HttpEntity<OAuthCodePayload> entity(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        OAuthCodePayload payload = new OAuthCodePayload(
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

    private boolean hasSuccessEnvelope(WpsEnvelope<?> response) {
        if (response == null) {
            return false;
        }
        if (response.getCode() == null) {
            return false;
        }
        return response.getCode().intValue() == 0;
    }

    private OffsetDateTime parseExpireAt(String expireAt) {
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
}
