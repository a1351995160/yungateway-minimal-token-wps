package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
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

class WpsFileClientTest {

    @Test
    void sendsBearerTokenWhenListingFiles() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"items\":[{\"fileId\":\"wps-file-001\","
                + "\"name\":\"demo.docx\",\"type\":\"WORD\",\"folder\":false,"
                + "\"updatedAt\":\"2026-05-26T18:00:00+08:00\"}],\"nextCursor\":\"next\"}}";
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsFileList list = client.listFiles(request());

        assertThat(list.getItems()).hasSize(1);
        assertThat(list.getItems().get(0).getFileId()).isEqualTo("wps-file-001");
        assertThat(list.getNextCursor()).isEqualTo("next");
        server.verify();
    }

    @Test
    void mapsWpsFailureToStableErrorCode() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andRespond(withServerError());
        WpsFileListRequest listRequest = request();

        assertThatThrownBy(() -> client.listFiles(listRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    private WpsFileListRequest request() {
        return new WpsFileListRequest("user-token", "root", 20, null);
    }

    private WpsClientProperties properties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.test");
        properties.setFileListPath("/api/user/files");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }
}
