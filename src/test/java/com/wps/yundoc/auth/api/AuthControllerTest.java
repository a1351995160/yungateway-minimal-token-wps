package com.wps.yundoc.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import com.wps.yundoc.testsupport.UserAssertionSigner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "yundoc.auth-token-rate-limit.max-failures-per-client=2",
        "yundoc.auth-token-rate-limit.max-failures-per-remote-address=100"
})
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issuesAppJwtByDefaultWithoutUserClaim() throws IOException {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-token-ok", "user-files:list");

        ResponseEntity<String> response = token(credentials.getClientId(), credentials.getClientSecret());

        JsonNode body = objectMapper.readTree(response.getBody());
        String accessToken = body.path("data").path("accessToken").asText();
        JsonNode payload = jwtPayload(accessToken);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("data").path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.path("data").path("identityType").asText()).isEqualTo("APP");
        assertThat(body.path("data").path("apiPermissions").get(0).asText()).isEqualTo("user-files:list");
        assertThat(payload.path("businessSystemId").asText()).isEqualTo("biz-token-ok");
        assertThat(payload.path("identityType").asText()).isEqualTo("APP");
        assertThat(payload.has("userId")).isFalse();
        assertThat(payload.has("authMode")).isFalse();
    }

    @Test
    void issuesUserJwtWhenUserIdentityIsRequested() throws Exception {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-token-user", "user-files:list");

        MvcResult result = signedUserToken(credentials, "user-001");

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        JsonNode payload = jwtPayload(data.path("accessToken").asText());
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(data.path("identityType").asText()).isEqualTo("USER");
        assertThat(data.path("userId").asText()).isEqualTo("user-001");
        assertThat(payload.path("identityType").asText()).isEqualTo("USER");
        assertThat(payload.path("userId").asText()).isEqualTo("user-001");
    }

    @Test
    void rejectsUserJwtRequestWithoutUserAssertion() throws Exception {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-token-user-unsigned", "user-files:list");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), credentials.getClientSecret(), "USER", "user-001")))
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString()).path("error");
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(error.path("code").asText()).isEqualTo("USER_ASSERTION_INVALID");
    }

    @Test
    void rejectsUserJwtRequestWithoutUserId() throws Exception {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-token-missing-user", "user-files:list");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), credentials.getClientSecret(), "USER", null)))
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString()).path("error");
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(error.path("code").asText()).isEqualTo("USER_ID_REQUIRED");
    }

    @Test
    void usesBusinessSystemJwtTtlWhenIssuingToken() throws IOException {
        BusinessSystemCredentials credentials = businessSystemFixture.enabled("biz-token-ttl", 600);

        ResponseEntity<String> response = token(credentials.getClientId(), credentials.getClientSecret());

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        JsonNode payload = jwtPayload(data.path("accessToken").asText());
        long actualTtl = payload.path("exp").asLong() - payload.path("iat").asLong();
        assertThat(data.path("expiresIn").asLong()).isEqualTo(600);
        assertThat(actualTtl).isEqualTo(600);
    }

    @Test
    void rejectsWrongClientSecret() throws Exception {
        BusinessSystemCredentials credentials = businessSystemFixture.enabled("biz-token-wrong-secret");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), "wrong-secret")))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void rateLimitsRepeatedWrongClientSecretAttempts() throws Exception {
        BusinessSystemCredentials credentials = businessSystemFixture.enabled("biz-token-rate-limit");

        rejectWrongSecret(credentials);
        rejectWrongSecret(credentials);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), "wrong-secret")))
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString()).path("error");
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(error.path("code").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void allowsTokenAfterSingleWrongClientSecretAttempt() throws Exception {
        BusinessSystemCredentials credentials = businessSystemFixture.enabled("biz-token-rate-limit-success");

        rejectWrongSecret(credentials);

        ResponseEntity<String> response = token(credentials.getClientId(), credentials.getClientSecret());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void rejectWrongSecret(BusinessSystemCredentials credentials) throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), "wrong-secret")))
                .andExpect(status().isUnauthorized());
    }

    private ResponseEntity<String> token(String clientId, String clientSecret) {
        return restTemplate.postForEntity(url("/api/v1/auth/token"), jsonEntity(tokenJson(clientId, clientSecret)), String.class);
    }

    private MvcResult signedUserToken(BusinessSystemCredentials credentials, String userId) throws java.lang.Exception {
        HttpHeaders headers = jsonHeaders();
        UserAssertionSigner.sign(headers, credentials, "POST", "/api/v1/auth/token", "", userId);
        return mockMvc.perform(post("/api/v1/auth/token")
                        .headers(headers)
                        .content(tokenJson(credentials.getClientId(), credentials.getClientSecret(), "USER", userId)))
                .andReturn();
    }

    private String tokenJson(String clientId, String clientSecret) {
        return "{\"clientId\":\"" + clientId + "\",\"clientSecret\":\"" + clientSecret + "\"}";
    }

    private String tokenJson(String clientId, String clientSecret, String identityType, String userId) {
        StringBuilder json = new StringBuilder("{\"clientId\":\"")
                .append(clientId)
                .append("\",\"clientSecret\":\"")
                .append(clientSecret)
                .append("\",\"identityType\":\"")
                .append(identityType)
                .append("\"");
        if (userId != null) {
            json.append(",\"userId\":\"").append(userId).append("\"");
        }
        return json.append("}").toString();
    }

    private HttpEntity<String> jsonEntity(String body) {
        return new HttpEntity<>(body, jsonHeaders());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private JsonNode jwtPayload(String accessToken) throws IOException {
        String[] parts = accessToken.split("\\.");
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
