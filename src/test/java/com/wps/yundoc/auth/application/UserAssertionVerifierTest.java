package com.wps.yundoc.auth.application;

import com.wps.yundoc.businesssystem.domain.WpsIdentityType;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
import com.wps.yundoc.common.crypto.YundocCryptoService;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.UserAssertionSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAssertionVerifierTest {

    private static final String BUSINESS_SYSTEM_ID = "biz-gm";
    private static final String CLIENT_ID = "cli-gm";
    private static final String USER_ID = "user-gm";
    private static final String SIGNING_KEY = "test-client-secret-pepper";
    private static final String METHOD = "POST";
    private static final String PATH = "/api/v1/auth/token";
    private static final String QUERY = "";

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void verifiesGmUserAssertionSignature() {
        UserAssertionVerifier verifier = verifier(true);
        MockHttpServletRequest request = signedRequest(YundocCryptoAlgorithms.HMAC_SM3, "nonce-gm");
        RequestContextHolder.set(requestContext());

        verifier.verify(request, USER_ID);
    }

    @Test
    void verifiesLegacyUserAssertionSignatureWhenEnabled() {
        UserAssertionVerifier verifier = verifier(true);
        MockHttpServletRequest request = signedRequest(YundocCryptoAlgorithms.HMAC_SHA256, "nonce-legacy");
        RequestContextHolder.set(requestContext());

        verifier.verify(request, USER_ID);
    }

    @Test
    void rejectsLegacyUserAssertionSignatureWhenDisabled() {
        UserAssertionVerifier verifier = verifier(false);
        MockHttpServletRequest request = signedRequest(YundocCryptoAlgorithms.HMAC_SHA256, "nonce-disabled");
        RequestContextHolder.set(requestContext());

        assertThatThrownBy(() -> verifier.verify(request, USER_ID))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.USER_ASSERTION_INVALID);
    }

    private UserAssertionVerifier verifier(boolean legacyEnabled) {
        ClientSecretDigestProperties digestProperties = new ClientSecretDigestProperties();
        digestProperties.setPepper(SIGNING_KEY);
        UserAssertionProperties assertionProperties = new UserAssertionProperties();
        assertionProperties.setLegacySignatureEnabled(legacyEnabled);
        return new UserAssertionVerifier(
                digestProperties,
                assertionProperties,
                new UserAssertionNonceCache(assertionProperties),
                new YundocCryptoService());
    }

    private MockHttpServletRequest signedRequest(String algorithm, String nonce) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        HttpHeaders headers = new HttpHeaders();
        headers.set(UserAssertionVerifier.USER_ID_HEADER, USER_ID);
        headers.set(UserAssertionVerifier.TIMESTAMP_HEADER, timestamp);
        headers.set(UserAssertionVerifier.NONCE_HEADER, nonce);
        headers.set(UserAssertionVerifier.SIGNATURE_HEADER, UserAssertionSigner.signature(
                credentials(),
                signingInput(timestamp, nonce),
                algorithm));
        MockHttpServletRequest request = new MockHttpServletRequest(METHOD, PATH);
        headers.forEach((name, values) -> values.forEach(value -> request.addHeader(name, value)));
        return request;
    }

    private String signingInput(String timestamp, String nonce) {
        return METHOD + "\n"
                + PATH + "\n"
                + QUERY + "\n"
                + BUSINESS_SYSTEM_ID + "\n"
                + CLIENT_ID + "\n"
                + USER_ID + "\n"
                + timestamp + "\n"
                + nonce;
    }

    private BusinessSystemCredentials credentials() {
        return new BusinessSystemCredentials(BUSINESS_SYSTEM_ID, CLIENT_ID, "secret", SIGNING_KEY);
    }

    private RequestContext requestContext() {
        return RequestContext.builder("request-gm")
                .businessSystemId(BUSINESS_SYSTEM_ID)
                .clientId(CLIENT_ID)
                .identityType(WpsIdentityType.USER)
                .userId(USER_ID)
                .build();
    }
}
