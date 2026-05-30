package com.wps.yundoc.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void issuesBusinessJwtWithoutUserOrAuthModeClaims() throws IOException {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-token-ok", "user-files:list");

        ResponseEntity<String> response = token(credentials.getClientId(), credentials.getClientSecret());

        JsonNode body = objectMapper.readTree(response.getBody());
        String accessToken = body.path("data").path("accessToken").asText();
        JsonNode payload = jwtPayload(accessToken);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("data").path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.path("data").path("apiPermissions").get(0).asText()).isEqualTo("user-files:list");
        assertThat(payload.path("businessSystemId").asText()).isEqualTo("biz-token-ok");
        assertThat(payload.has("userId")).isFalse();
        assertThat(payload.has("authMode")).isFalse();
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

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), "wrong-secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitsRepeatedWrongClientSecretAttempts() throws Exception {
        BusinessSystemCredentials credentials = businessSystemFixture.enabled("biz-token-rate-limit");

        rejectWrongSecret(credentials);
        rejectWrongSecret(credentials);

        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenJson(credentials.getClientId(), "wrong-secret")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
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

    private String tokenJson(String clientId, String clientSecret) {
        return "{\"clientId\":\"" + clientId + "\",\"clientSecret\":\"" + clientSecret + "\"}";
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
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
