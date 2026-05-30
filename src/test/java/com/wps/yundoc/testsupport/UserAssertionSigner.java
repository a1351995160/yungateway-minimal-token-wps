package com.wps.yundoc.testsupport;

import com.wps.yundoc.auth.application.UserAssertionVerifier;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class UserAssertionSigner {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private UserAssertionSigner() {
    }

    public static void sign(
            HttpHeaders headers,
            BusinessSystemCredentials credentials,
            String method,
            String path,
            String queryString,
            String userId) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        sign(headers, credentials, method, path, queryString, userId, timestamp, nonce);
    }

    public static void sign(
            HttpHeaders headers,
            BusinessSystemCredentials credentials,
            String method,
            String path,
            String queryString,
            String userId,
            String timestamp,
            String nonce) {
        headers.set(UserAssertionVerifier.USER_ID_HEADER, userId);
        headers.set(UserAssertionVerifier.TIMESTAMP_HEADER, timestamp);
        headers.set(UserAssertionVerifier.NONCE_HEADER, nonce);
        headers.set(UserAssertionVerifier.SIGNATURE_HEADER, signature(
                credentials,
                method + "\n" + path + "\n" + queryString + "\n" + userId + "\n" + timestamp + "\n" + nonce));
    }

    private static String signature(BusinessSystemCredentials credentials, String signingInput) {
        try {
            Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
            signer.initSign(credentials.getUserAssertionPrivateKey());
            signer.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        } catch (GeneralSecurityException ex) {
            throw new AssertionError("user assertion signing failed", ex);
        }
    }
}
