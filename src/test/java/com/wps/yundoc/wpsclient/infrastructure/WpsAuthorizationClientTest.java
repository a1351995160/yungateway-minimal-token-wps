package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WpsAuthorizationClientTest {

    @Test
    void buildsAuthorizeUrlWithStateAndConfiguredScope() {
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(properties(), new RestTemplateBuilder());

        String authorizeUrl = client.authorizeUrl("state-001");

        assertThat(authorizeUrl).contains(
                "https://wps.test/oauth/authorize",
                "client_id=wps-app",
                "scope=files.read",
                "state=state-001");
    }

    @Test
    void exchangesCodeForUserToken() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(
                properties(),
                new RestTemplateBuilder(),
                restTemplate);
        String body = "{\"code\":0,\"data\":{\"accessToken\":\"user-token\","
                + "\"expireAt\":\"2026-05-26T18:00:00+08:00\"}}";
        server.expect(once(), requestTo("https://wps.test/oauth/user-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsUserToken token = client.exchangeCode("ok-code");

        assertThat(token.getAccessToken()).isEqualTo("user-token");
        server.verify();
    }

    @Test
    void mapsMissingExpireAtToUpstreamError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(
                properties(),
                new RestTemplateBuilder(),
                restTemplate);
        String body = "{\"code\":0,\"data\":{\"accessToken\":\"user-token\"}}";
        server.expect(once(), requestTo("https://wps.test/oauth/user-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCode("ok-code"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    private WpsClientProperties properties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.test");
        properties.setAuthorizePath("/oauth/authorize");
        properties.setUserTokenPath("/oauth/user-token");
        properties.setAppId("wps-app");
        properties.setRedirectUri("https://gateway.test/api/v1/wps/oauth/callback");
        properties.setOauthScope("files.read");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }
}
