package com.wps.yundoc.testsupport;

import com.wps.yundoc.auth.application.UserAssertionVerifier;
import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class UserAssertionSigner {

    private static final String JCA_HMAC_SHA256 = "HmacSHA256";

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
        return signature(credentials, signingInput, YundocCryptoAlgorithms.HMAC_SM3);
    }

    public static String signature(
            BusinessSystemCredentials credentials,
            String signingInput,
            String algorithm) {
        if (YundocCryptoAlgorithms.HMAC_SHA256.equals(algorithm)) {
            return hmacSha256(credentials, signingInput);
        }
        if (YundocCryptoAlgorithms.HMAC_SM3.equals(algorithm)) {
            return hmacSm3(credentials, signingInput);
        }
        throw new AssertionError("unsupported user assertion signing algorithm");
    }

    private static String hmacSha256(BusinessSystemCredentials credentials, String signingInput) {
        try {
            Mac mac = Mac.getInstance(JCA_HMAC_SHA256);
            byte[] key = credentials.getUserAssertionSigningKey().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, JCA_HMAC_SHA256));
            return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new AssertionError("user assertion signing failed", ex);
        }
    }

    private static String hmacSm3(BusinessSystemCredentials credentials, String signingInput) {
        HMac mac = new HMac(new SM3Digest());
        byte[] key = credentials.getUserAssertionSigningKey().getBytes(StandardCharsets.UTF_8);
        mac.init(new KeyParameter(key));
        byte[] input = signingInput.getBytes(StandardCharsets.UTF_8);
        mac.update(input, 0, input.length);
        byte[] result = new byte[mac.getMacSize()];
        mac.doFinal(result, 0);
        return base64Url(result);
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
