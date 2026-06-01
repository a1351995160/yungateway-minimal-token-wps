package com.wps.yundoc.mvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import com.wps.yundoc.testsupport.UserAssertionSigner;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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
        String appAccessToken = appAccessToken(credentials);
        String userAccessToken = userAccessToken(credentials, "smoke-user");

        JsonNode preview = postAppPreview(appAccessToken);
        JsonNode reauth = getUserFiles(userAccessToken, HttpStatus.UNAUTHORIZED);
        String state = stateFromAuthorizeUrl(reauth);
        callback(state);
        JsonNode files = getUserFiles(userAccessToken, HttpStatus.OK);

        assertThat(preview.path("data").path("previewUrl").asText()).startsWith("https://preview.test/files/");
        assertThat(preview.path("data").path("expireAt").asText()).isNotBlank();
        assertThat(reauth.path("error").path("code").asText()).isEqualTo("REAUTH_REQUIRED");
        assertThat(reauth.path("error").path("details").path("authorizeUrl").asText()).contains("state=");
        assertThat(files.path("data").path("items")).hasSizeGreaterThan(0);
        assertThat(files.path("data").path("items").get(0).path("fileId").asText()).isNotBlank();
    }

    private String appAccessToken(BusinessSystemCredentials credentials) throws IOException {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/token"),
                jsonEntity("{\"clientId\":\"" + credentials.getClientId()
                        + "\",\"clientSecret\":\"" + credentials.getClientSecret() + "\"}"),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body(response).path("data").path("accessToken").asText();
    }

    private String userAccessToken(BusinessSystemCredentials credentials, String userId) throws IOException {
        String body = "{\"clientId\":\"" + credentials.getClientId()
                + "\",\"clientSecret\":\"" + credentials.getClientSecret()
                + "\",\"identityType\":\"USER\",\"userId\":\"" + userId + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/token"),
                signedJsonEntity(body, credentials, userId),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body(response).path("data").path("accessToken").asText();
    }

    private JsonNode postAppPreview(String accessToken) throws IOException {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/app/previews"),
                multipartBearer(accessToken),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body(response);
    }

    private JsonNode getUserFiles(String accessToken, HttpStatus expectedStatus) throws IOException {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/user/files"),
                HttpMethod.GET,
                bearer(accessToken),
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

    private HttpEntity<String> bearer(String accessToken) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartBearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart());
        body.add("expireSeconds", "3600");
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<ByteArrayResource> filePart() {
        ByteArrayResource resource = new ByteArrayResource("preview-content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "smoke.docx";
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new HttpEntity<>(resource, headers);
    }

    private HttpEntity<String> jsonEntity(String body) {
        return new HttpEntity<>(body, jsonHeaders());
    }

    private HttpEntity<String> signedJsonEntity(
            String body,
            BusinessSystemCredentials credentials,
            String userId) {
        HttpHeaders headers = jsonHeaders();
        UserAssertionSigner.sign(headers, credentials, "POST", "/api/v1/auth/token", "", userId);
        return new HttpEntity<>(body, headers);
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
