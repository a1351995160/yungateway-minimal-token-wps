package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WpsAuthorizationClientTest {

    @Test
    void buildsAuthorizeUrlWithStateAndConfiguredScope() {
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(properties(), new RestTemplateBuilder());

        String authorizeUrl = client.authorizeUrl("state-001");

        assertThat(authorizeUrl).contains(
                "https://wps.test/oauth2/auth",
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
        String body = userTokenBody("user-token", "refresh-token");
        server.expect(once(), requestTo("https://wps.test/oauth2/token"))
                .andExpect(request -> assertThat(request.getHeaders().getContentType())
                        .isNotNull()
                        .matches(MediaType.APPLICATION_FORM_URLENCODED::isCompatibleWith))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(WpsRequestSigner.KSO_AUTHORIZATION_HEADER))
                        .isNull())
                .andExpect(content().string(allOf(
                        containsString("grant_type=authorization_code"),
                        containsString("client_id=wps-app"),
                        containsString("client_secret=wps-secret"),
                        containsString("code=ok-code"))))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsUserToken token = client.exchangeCode("ok-code");

        assertThat(token.getAccessToken()).isEqualTo("user-token");
        assertThat(token.getRefreshToken()).isEqualTo("refresh-token");
        server.verify();
    }

    @Test
    void mapsMissingExpiresInToUpstreamError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(
                properties(),
                new RestTemplateBuilder(),
                restTemplate);
        String body = "{\"access_token\":\"user-token\",\"refresh_token\":\"refresh-token\"}";
        server.expect(once(), requestTo("https://wps.test/oauth2/token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCode("ok-code"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void retriesTransientServerErrorWhenExchangingCode() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(
                properties(),
                new RestTemplateBuilder(),
                restTemplate);
        String body = userTokenBody("user-token", "refresh-token");
        server.expect(once(), requestTo("https://wps.test/oauth2/token"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("https://wps.test/oauth2/token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsUserToken token = client.exchangeCode("ok-code");

        assertThat(token.getAccessToken()).isEqualTo("user-token");
        server.verify();
    }

    @Test
    void refreshesUserTokenWithRefreshGrant() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsAuthorizationHttpClient client = new WpsAuthorizationHttpClient(
                properties(),
                new RestTemplateBuilder(),
                restTemplate);
        server.expect(once(), requestTo("https://wps.test/oauth2/token"))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                        .contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string(allOf(
                        containsString("grant_type=refresh_token"),
                        containsString("refresh_token=old-refresh"),
                        containsString("client_id=wps-app"))))
                .andRespond(withSuccess(userTokenBody("new-user-token", "new-refresh-token"), MediaType.APPLICATION_JSON));

        WpsUserToken token = client.refreshToken("old-refresh");

        assertThat(token.getAccessToken()).isEqualTo("new-user-token");
        assertThat(token.getRefreshToken()).isEqualTo("new-refresh-token");
        server.verify();
    }

    private WpsClientProperties properties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.test");
        properties.setAuthorizePath("/oauth2/auth");
        properties.setUserTokenPath("/oauth2/token");
        properties.setAppId("wps-app");
        properties.setAppSecret("wps-secret");
        properties.setRedirectUri("https://gateway.test/api/v1/wps/oauth/callback");
        properties.setOauthScope("files.read");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }

    private String userTokenBody(String accessToken, String refreshToken) {
        return "{\"access_token\":\"" + accessToken + "\","
                + "\"expires_in\":7200,"
                + "\"refresh_token\":\"" + refreshToken + "\","
                + "\"refresh_expires_in\":31536000,"
                + "\"token_type\":\"bearer\"}";
    }
}
