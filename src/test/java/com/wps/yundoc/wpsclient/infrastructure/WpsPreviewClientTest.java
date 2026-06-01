package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WpsPreviewClientTest {

    @Test
    void sendsBearerTokenWhenCreatingPreview() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = previewBody("https://preview/file", 1800);
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andExpect(header("Authorization", "Bearer app-token"))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(WpsRequestSigner.KSO_DATE_HEADER))
                        .isNotBlank())
                .andExpect(request -> assertThat(request.getHeaders().getFirst(WpsRequestSigner.KSO_AUTHORIZATION_HEADER))
                        .startsWith("KSO-1 wps-app:"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsPreviewLink link = client.createPreview(request());

        assertThat(link.getPreviewUrl()).isEqualTo("https://preview/file");
        server.verify();
    }

    @Test
    void mapsWpsFailureToStableErrorCode() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        WpsPreviewRequest previewRequest = request();
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.createPreview(previewRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void mapsInvalidSuccessPayloadToStableErrorCode() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        WpsPreviewRequest previewRequest = request();
        String body = "{\"code\":0,\"data\":{\"previewUrl\":\"https://preview/file\",\"expireAt\":\"bad-date\"}}";
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createPreview(previewRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void retriesTransientServerErrorWhenCreatingPreview() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = previewBody("https://preview/file", 1800);
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsPreviewLink link = client.createPreview(request());

        assertThat(link.getPreviewUrl()).isEqualTo("https://preview/file");
        server.verify();
    }

    @Test
    void rejectsPreviewUrlOutsideAllowedHosts() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        WpsPreviewRequest previewRequest = request();
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withSuccess(previewBody("https://evil.test/file", 1800), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createPreview(previewRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void rejectsPreviewUrlLongerThanRequestedTtl() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        WpsPreviewRequest previewRequest = request();
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withSuccess(previewBody("https://preview/file", 7200), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createPreview(previewRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void fetchesAppTokenFromWps() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"access_token\":\"app-token\",\"expires_in\":7200,\"token_type\":\"bearer\"}";
        server.expect(once(), requestTo("https://wps.test/oauth2/token"))
                .andExpect(request -> assertThat(request.getHeaders().getContentType())
                        .isNotNull()
                        .matches(MediaType.APPLICATION_FORM_URLENCODED::isCompatibleWith))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(WpsRequestSigner.KSO_AUTHORIZATION_HEADER))
                        .isNull())
                .andExpect(content().string(allOf(
                        containsString("grant_type=client_credentials"),
                        containsString("client_id=wps-app"),
                        containsString("client_secret=wps-secret"))))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThat(client.issueAppToken().getAccessToken()).isEqualTo("app-token");
    }

    @Test
    void rejectsInsecureWpsBaseUrlWhenBuildingClient() {
        WpsClientProperties properties = properties();
        properties.setBaseUrl("http://wps.test");
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        assertThatThrownBy(() -> new WpsHttpClient(properties, restTemplateBuilder))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void rejectsWpsBaseUrlWithUserInfoWhenBuildingClient() {
        WpsClientProperties properties = properties();
        properties.setBaseUrl("https://user@wps.test");
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        assertThatThrownBy(() -> new WpsHttpClient(properties, restTemplateBuilder))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    private WpsPreviewRequest request() {
        return new WpsPreviewRequest("wps-file-001", 3600, "app-token");
    }

    private WpsClientProperties properties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.test");
        properties.setPreviewPath("/api/preview-links");
        properties.setTokenPath("/oauth2/token");
        properties.setAppId("wps-app");
        properties.setAppSecret("wps-secret");
        properties.setPreviewUrlAllowedHosts(Collections.singletonList("preview"));
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        properties.setMaxRetries(1);
        return properties;
    }

    private WpsClientProperties noRetryProperties() {
        WpsClientProperties properties = properties();
        properties.setMaxRetries(0);
        return properties;
    }

    private String previewBody(String previewUrl, long expireInSeconds) {
        String expireAt = OffsetDateTime.now().plusSeconds(expireInSeconds).toString();
        return "{\"code\":0,\"data\":{\"previewUrl\":\"" + previewUrl
                + "\",\"expireAt\":\"" + expireAt + "\"}}";
    }
}
