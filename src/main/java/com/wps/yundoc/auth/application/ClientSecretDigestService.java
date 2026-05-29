package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class ClientSecretDigestService {

    private static final String HMAC_SHA256 = "HMAC-SHA256";
    private static final String JCA_HMAC_SHA256 = "HmacSHA256";

    private final ClientSecretDigestProperties properties;
    private final SecretGenerator secretGenerator;

    public ClientSecretDigestService(
            ClientSecretDigestProperties properties,
            SecretGenerator secretGenerator) {
        this.properties = properties;
        this.secretGenerator = secretGenerator;
    }

    public ClientSecretDigest digestNew(String clientSecret) {
        String salt = secretGenerator.generateSalt();
        String digest = digest(clientSecret, salt, properties.getAlgorithm());
        return new ClientSecretDigest(digest, salt, properties.getAlgorithm());
    }

    public boolean matches(String rawSecret, String salt, String algorithm, String expectedDigest) {
        String actualDigest = digest(rawSecret, salt, algorithm);
        byte[] actual = actualDigest.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedDigest.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    public String digest(String rawSecret, String salt, String algorithm) {
        assertSupported(algorithm);
        return hmacSha256(rawSecret + ":" + salt);
    }

    private void assertSupported(String algorithm) {
        if (!HMAC_SHA256.equals(algorithm)) {
            throw new YundocException(YundocErrorCode.VALIDATION_FAILED, "Unsupported secret digest algorithm");
        }
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance(JCA_HMAC_SHA256);
            byte[] key = properties.getPepper().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, JCA_HMAC_SHA256));
            return HexStrings.encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Secret digest failed", ex);
        }
    }
}
