package com.wps.yundoc.auth.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import com.wps.yundoc.testsupport.UserAssertionSigner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Test
    void buildsRequestContextBeforeCapabilityController() throws IOException {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-filter-ok", "user-files:list");
        String token = userAccessToken(credentials, "user-filter-ok");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/test/capability"),
                HttpMethod.GET,
                authorized(token),
                String.class);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("data").path("businessSystemId").asText()).isEqualTo("biz-filter-ok");
        assertThat(body.path("data").path("apiCode").asText()).isEqualTo("user-files:list");
    }

    @Test
    void rejectsCapabilityRequestWithoutPermission() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-filter-denied", "app-preview:create");
        String token = userAccessToken(credentials, "user-filter-denied");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/test/capability"),
                HttpMethod.GET,
                authorized(token),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsAppJwtOnUserCapabilityRoute() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-filter-app-on-user", "user-files:list");
        String token = appAccessToken(credentials);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/test/capability"),
                HttpMethod.GET,
                authorized(token),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String appAccessToken(BusinessSystemCredentials credentials) {
        String body = "{\"clientId\":\"" + credentials.getClientId()
                + "\",\"clientSecret\":\"" + credentials.getClientSecret() + "\"}";
        return tokenFromBody(body);
    }

    private String userAccessToken(BusinessSystemCredentials credentials, String userId) {
        String body = "{\"clientId\":\"" + credentials.getClientId()
                + "\",\"clientSecret\":\"" + credentials.getClientSecret()
                + "\",\"identityType\":\"USER\",\"userId\":\"" + userId + "\"}";
        return tokenFromBody(signedJsonEntity(body, credentials, userId));
    }

    private String tokenFromBody(String body) {
        return tokenFromBody(jsonEntity(body));
    }

    private String tokenFromBody(HttpEntity<String> entity) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/auth/token"),
                entity,
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

    private HttpEntity<String> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>("{}", headers);
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> signedJsonEntity(
            String body,
            BusinessSystemCredentials credentials,
            String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        UserAssertionSigner.sign(headers, credentials, "POST", "/api/v1/auth/token", "", userId);
        return new HttpEntity<>(body, headers);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @TestConfiguration
    static class CapabilityTestConfiguration {

        @Bean
        CapabilityTestController capabilityTestController() {
            return new CapabilityTestController();
        }

        @Bean
        @Primary
        CapabilityRoutePolicy testCapabilityRoutePolicy() {
            return new TestCapabilityRoutePolicy();
        }
    }

    @RestController
    static class CapabilityTestController {

        @GetMapping("/api/v1/test/capability")
        public ApiResponse<CapabilityContextResponse> preview() {
            RequestContext context = RequestContextHolder.current()
                    .orElseThrow(() -> new IllegalStateException("request context is required"));
            CapabilityContextResponse response = new CapabilityContextResponse(
                    context.getBusinessSystemId(),
                    context.getApiCode());
            return ApiResponse.success(response, context.getRequestId());
        }
    }

    static class TestCapabilityRoutePolicy extends CapabilityRoutePolicy {

        @Override
        public Optional<String> resolve(HttpServletRequest request) {
            if ("/api/v1/test/capability".equals(request.getRequestURI())) {
                return Optional.of("user-files:list");
            }
            return Optional.empty();
        }
    }

    static class CapabilityContextResponse {

        private final String businessSystemId;
        private final String apiCode;

        CapabilityContextResponse(String businessSystemId, String apiCode) {
            this.businessSystemId = businessSystemId;
            this.apiCode = apiCode;
        }

        public String getBusinessSystemId() {
            return businessSystemId;
        }

        public String getApiCode() {
            return apiCode;
        }
    }
}
