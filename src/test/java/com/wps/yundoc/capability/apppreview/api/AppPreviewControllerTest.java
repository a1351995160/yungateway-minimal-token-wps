package com.wps.yundoc.capability.apppreview.api;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AppPreviewControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Test
    void createsAppPreviewWhenBusinessSystemHasPermission() throws IOException {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-app-preview-ok", "app-preview:create");
        String token = accessToken(credentials);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/app/previews"),
                HttpMethod.POST,
                authorizedMultipart(token, "contract.docx", null, 3600),
                String.class);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.path("previewUrl").asText()).contains("mock-uploaded-file");
        assertThat(data.path("fileId").asText()).isEqualTo("mock-uploaded-file");
        assertThat(data.path("expireAt").asText()).isNotBlank();
    }

    @Test
    void rejectsInvalidPreviewRequestBody() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-app-preview-invalid", "app-preview:create");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/app/previews"),
                HttpMethod.POST,
                authorizedMultipartWithoutFile(accessToken(credentials)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInvalidPreviewFileIdShape() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-app-preview-invalid-file-id", "app-preview:create");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/app/previews"),
                HttpMethod.POST,
                authorizedMultipart(accessToken(credentials), "contract.docx", "../secret.docx", 3600),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAppPreviewWithoutPermission() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-app-preview-denied", "user-files:list");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/app/previews"),
                HttpMethod.POST,
                authorizedMultipart(accessToken(credentials), "contract.docx", null, 3600),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsTrailingSlashPreviewWithoutAuthorization() throws IOException {
        int statusCode = postWithoutAuthorization(url("/api/v1/app/previews/"));

        assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private String accessToken(BusinessSystemCredentials credentials) {
        String body = "{\"clientId\":\"" + credentials.getClientId()
                + "\",\"clientSecret\":\"" + credentials.getClientSecret() + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/token"),
                jsonEntity(body),
                String.class);
        return readAccessToken(response);
    }

    private String readAccessToken(ResponseEntity<String> response) {
        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            return body.path("data").path("accessToken").asText();
        } catch (IOException ex) {
            throw new AssertionError("token response must be json", ex);
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> authorizedMultipart(
            String token,
            String fileName,
            String displayName,
            int expireSeconds) {
        return multipart(token, fileName, displayName, expireSeconds);
    }

    private HttpEntity<MultiValueMap<String, Object>> authorizedMultipartWithoutFile(String token) {
        HttpHeaders headers = multipartHeaders(token);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("expireSeconds", "3600");
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<MultiValueMap<String, Object>> multipart(
            String token,
            String fileName,
            String displayName,
            int expireSeconds) {
        HttpHeaders headers = multipartHeaders(token);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart(fileName));
        if (displayName != null) {
            body.add("displayName", displayName);
        }
        body.add("expireSeconds", String.valueOf(expireSeconds));
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders multipartHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private HttpEntity<ByteArrayResource> filePart(String fileName) {
        ByteArrayResource resource = new ByteArrayResource("preview-content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new HttpEntity<>(resource, headers);
    }

    private HttpEntity<String> jsonEntity(String body) {
        return new HttpEntity<>(body, jsonHeaders());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private int postWithoutAuthorization(String targetUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
        return connection.getResponseCode();
    }
}
