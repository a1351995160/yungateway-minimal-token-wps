package com.wps.yundoc.testsupport;

import com.wps.yundoc.auth.application.UserAssertionVerifier;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class UserAssertionSigner {

    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

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
                method + "\n" + path + "\n" + queryString + "\n"
                        + credentials.getBusinessSystemId() + "\n"
                        + credentials.getClientId() + "\n"
                        + userId + "\n" + timestamp + "\n" + nonce));
    }

    private static String signature(BusinessSystemCredentials credentials, String signingInput) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            byte[] key = credentials.getUserAssertionSigningKey().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, SIGNATURE_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new AssertionError("user assertion signing failed", ex);
        }
    }
}
