package com.wps.yundoc.mvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MvpSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Test
    void completesMvpFlowWithMockWps() throws IOException {
        BusinessSystemCredentials credentials = businessSystemFixture.enabled(
                "biz-mvp-smoke",
                "app-preview:create",
                "user-files:list");
        String accessToken = accessToken(credentials);

        JsonNode preview = postAppPreview(accessToken);
        JsonNode reauth = getUserFiles(accessToken, HttpStatus.UNAUTHORIZED);
        String state = stateFromAuthorizeUrl(reauth);
        callback(state);
        JsonNode files = getUserFiles(accessToken, HttpStatus.OK);

        assertThat(preview.path("data").path("previewUrl").asText()).startsWith("https://preview.test/files/");
        assertThat(preview.path("data").path("expireAt").asText()).isNotBlank();
        assertThat(reauth.path("error").path("code").asText()).isEqualTo("REAUTH_REQUIRED");
        assertThat(reauth.path("error").path("details").path("authorizeUrl").asText()).contains("state=");
        assertThat(files.path("data").path("items")).hasSizeGreaterThan(0);
        assertThat(files.path("data").path("items").get(0).path("fileId").asText()).isNotBlank();
    }

    private String accessToken(BusinessSystemCredentials credentials) throws IOException {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/token"),
                jsonEntity("{\"clientId\":\"" + credentials.getClientId()
                        + "\",\"clientSecret\":\"" + credentials.getClientSecret() + "\"}"),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body(response).path("data").path("accessToken").asText();
    }

    private JsonNode postAppPreview(String accessToken) throws IOException {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/app/previews"),
                bearer(accessToken, previewJson("wps-file-001")),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body(response);
    }

    private JsonNode getUserFiles(String accessToken, HttpStatus expectedStatus) throws IOException {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/user/files?userId=smoke-user"),
                HttpMethod.GET,
                bearer(accessToken, null),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return body(response);
    }

    private String stateFromAuthorizeUrl(JsonNode reauth) {
        String authorizeUrl = reauth.path("error").path("details").path("authorizeUrl").asText();
        return UriComponentsBuilder.fromUriString(authorizeUrl).build().getQueryParams().getFirst("state");
    }

    private void callback(String state) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                URI.create(url("/api/v1/wps/oauth/callback?code=mock-code&state=" + state)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String previewJson(String fileId) {
        return "{\"source\":{\"type\":\"WPS_FILE\",\"fileId\":\"" + fileId
                + "\"},\"options\":{\"expireSeconds\":3600}}";
    }

    private HttpEntity<String> bearer(String accessToken, String body) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> jsonEntity(String body) {
        return new HttpEntity<>(body, jsonHeaders());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private JsonNode body(ResponseEntity<String> response) throws IOException {
        return objectMapper.readTree(response.getBody());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
