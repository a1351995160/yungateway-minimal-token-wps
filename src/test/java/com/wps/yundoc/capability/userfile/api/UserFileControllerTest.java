package com.wps.yundoc.capability.userfile.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import com.wps.yundoc.testsupport.UserAssertionSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserFileControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Test
    void returnsReauthWhenUserTokenMissing() throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials("biz-user-files-reauth");
        String token = accessToken(credentials);

        ResponseEntity<String> response = getFiles(credentials, token, "userId=user-001");

        JsonNode error = objectMapper.readTree(response.getBody()).path("error");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.path("code").asText()).isEqualTo("REAUTH_REQUIRED");
        assertThat(error.path("details").path("authorizeUrl").asText()).contains("state=");
    }

    @ParameterizedTest
    @MethodSource("invalidUserFileQueries")
    void rejectsInvalidUserFileQuery(
            String businessSystemId,
            String query,
            String expectedCode) throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials(businessSystemId);
        String token = accessToken(credentials);

        ResponseEntity<String> response = getFiles(token, query);

        JsonNode error = objectMapper.readTree(response.getBody()).path("error");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.path("code").asText()).isEqualTo(expectedCode);
    }

    @Test
    void callbackStoresTokenAndFileListSucceeds() throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials("biz-user-files-success");
        String token = accessToken(credentials);
        ResponseEntity<String> first = getFiles(credentials, token, "userId=user-003");
        String state = stateFrom(first);

        ResponseEntity<String> callback = restTemplate.getForEntity(
                url("/api/v1/wps/oauth/callback?code=ok-code&state=" + state),
                String.class);
        ResponseEntity<String> second = getFiles(credentials, token, "userId=user-003&parentFileId=root&limit=20");

        JsonNode data = objectMapper.readTree(second.getBody()).path("data");
        assertThat(callback.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.path("items").get(0).path("fileId").asText()).isEqualTo("wps-file-001");
        assertThat(data.path("nextCursor").asText()).isEqualTo("next-cursor");
    }

    @Test
    void rejectsUserFileRequestWithoutUserAssertion() throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials("biz-user-files-no-assertion");
        String token = accessToken(credentials);

        ResponseEntity<String> response = getFiles(token, "?userId=user-004");

        JsonNode error = objectMapper.readTree(response.getBody()).path("error");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.path("code").asText()).isEqualTo("USER_ASSERTION_INVALID");
    }

    @Test
    void rejectsUserFileRequestWhenAssertionUserIdDiffersFromQuery() throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials("biz-user-files-user-mismatch");
        String token = accessToken(credentials);

        ResponseEntity<String> response = getFiles(credentials, token, "userId=user-005", "user-006");

        JsonNode error = objectMapper.readTree(response.getBody()).path("error");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.path("code").asText()).isEqualTo("USER_ASSERTION_INVALID");
    }

    @Test
    void rejectsReplayedUserAssertionNonce() throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials("biz-user-files-replay");
        String token = accessToken(credentials);
        String queryString = "userId=user-007";
        HttpEntity<String> signedRequest = signedRequest(
                credentials,
                token,
                queryString,
                "user-007",
                String.valueOf(Instant.now().getEpochSecond()),
                "fixed-replay-nonce");

        ResponseEntity<String> first = restTemplate.exchange(
                url("/api/v1/user/files?" + queryString),
                HttpMethod.GET,
                signedRequest,
                String.class);
        ResponseEntity<String> second = restTemplate.exchange(
                url("/api/v1/user/files?" + queryString),
                HttpMethod.GET,
                signedRequest,
                String.class);

        JsonNode error = objectMapper.readTree(second.getBody()).path("error");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(objectMapper.readTree(first.getBody()).path("error").path("code").asText())
                .isEqualTo("REAUTH_REQUIRED");
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.path("code").asText()).isEqualTo("USER_ASSERTION_INVALID");
    }

    @Test
    void rejectsUserFileRequestWhenSignedQueryIsChanged() throws IOException {
        BusinessSystemCredentials credentials = userFileCredentials("biz-user-files-query-tamper");
        String token = accessToken(credentials);
        String signedQueryString = "userId=user-008";
        String actualQueryString = "userId=user-008&parentFileId=secret";
        HttpEntity<String> signedRequest = signedRequest(
                credentials,
                token,
                signedQueryString,
                "user-008",
                String.valueOf(Instant.now().getEpochSecond()),
                "fixed-query-nonce");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/user/files?" + actualQueryString),
                HttpMethod.GET,
                signedRequest,
                String.class);

        JsonNode error = objectMapper.readTree(response.getBody()).path("error");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.path("code").asText()).isEqualTo("USER_ASSERTION_INVALID");
    }

    private ResponseEntity<String> getFiles(String token, String query) {
        return restTemplate.exchange(
                url("/api/v1/user/files" + query),
                HttpMethod.GET,
                authorized(token),
                String.class);
    }

    private ResponseEntity<String> getFiles(
            BusinessSystemCredentials credentials,
            String token,
            String queryString) {
        return getFiles(credentials, token, queryString, queryUserId(queryString));
    }

    private ResponseEntity<String> getFiles(
            BusinessSystemCredentials credentials,
            String token,
            String queryString,
            String assertedUserId) {
        return restTemplate.exchange(
                url("/api/v1/user/files?" + queryString),
                HttpMethod.GET,
                authorized(credentials, token, "GET", "/api/v1/user/files", queryString, assertedUserId),
                String.class);
    }

    private static Stream<Arguments> invalidUserFileQueries() {
        return Stream.of(
                Arguments.of("biz-user-files-missing-user", "", "USER_ID_REQUIRED"),
                Arguments.of(
                        "biz-user-files-bad-user",
                        "?userId=user-001&userId=user-002",
                        "VALIDATION_FAILED"),
                Arguments.of("biz-user-files-invalid-shape", "?userId=bad user", "VALIDATION_FAILED"),
                Arguments.of("biz-user-files-blank-user", "?userId=%20", "VALIDATION_FAILED"));
    }

    private String stateFrom(ResponseEntity<String> response) throws IOException {
        JsonNode error = objectMapper.readTree(response.getBody()).path("error");
        URI uri = URI.create(error.path("details").path("authorizeUrl").asText());
        return uri.getQuery().replaceFirst("^.*state=([^&]+).*$", "$1");
    }

    private BusinessSystemCredentials userFileCredentials(String businessSystemId) {
        return businessSystemFixture.enabled(businessSystemId, "user-files:list");
    }

    private String accessToken(BusinessSystemCredentials credentials) {
        String body = "{\"clientId\":\"" + credentials.getClientId()
                + "\",\"clientSecret\":\"" + credentials.getClientSecret() + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/token"),
                json(body),
                String.class);
        return readAccessToken(response);
    }

    private String readAccessToken(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody()).path("data").path("accessToken").asText();
        } catch (IOException ex) {
            throw new AssertionError("token response must be json", ex);
        }
    }

    private HttpEntity<String> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> authorized(
            BusinessSystemCredentials credentials,
            String token,
            String method,
            String path,
            String queryString,
            String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        UserAssertionSigner.sign(headers, credentials, method, path, queryString, userId);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> signedRequest(
            BusinessSystemCredentials credentials,
            String token,
            String queryString,
            String userId,
            String timestamp,
            String nonce) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        UserAssertionSigner.sign(
                headers,
                credentials,
                "GET",
                "/api/v1/user/files",
                queryString,
                userId,
                timestamp,
                nonce);
        return new HttpEntity<>(headers);
    }

    private String queryUserId(String queryString) {
        return queryString.replaceFirst("^.*userId=([^&]+).*$", "$1");
    }

    private HttpEntity<String> json(String body) {
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
}
