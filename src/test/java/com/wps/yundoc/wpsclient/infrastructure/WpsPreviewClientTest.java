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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
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
        String body = "{\"code\":0,\"data\":{\"previewUrl\":\"https://preview\",\"expireAt\":\"2026-05-26T18:00:00+08:00\"}}";
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andExpect(header("Authorization", "Bearer app-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsPreviewLink link = client.createPreview(request());

        assertThat(link.getPreviewUrl()).isEqualTo("https://preview");
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
        String body = "{\"code\":0,\"data\":{\"previewUrl\":\"https://preview\",\"expireAt\":\"bad-date\"}}";
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
        String body = "{\"code\":0,\"data\":{\"previewUrl\":\"https://preview\",\"expireAt\":\"2026-05-26T18:00:00+08:00\"}}";
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("https://wps.test/api/preview-links"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsPreviewLink link = client.createPreview(request());

        assertThat(link.getPreviewUrl()).isEqualTo("https://preview");
        server.verify();
    }

    @Test
    void fetchesAppTokenFromWps() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsHttpClient client = new WpsHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"accessToken\":\"app-token\",\"expireAt\":\"2026-05-26T18:00:00+08:00\"}}";
        server.expect(once(), requestTo("https://wps.test/oauth/token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThat(client.issueAppToken().getAccessToken()).isEqualTo("app-token");
    }

    private WpsPreviewRequest request() {
        return new WpsPreviewRequest("wps-file-001", 3600, "app-token");
    }

    private WpsClientProperties properties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.test");
        properties.setPreviewPath("/api/preview-links");
        properties.setTokenPath("/oauth/token");
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
}
