package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
import com.wps.yundoc.common.crypto.YundocCryptoService;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * ClientSecretDigestService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class ClientSecretDigestService {

    private static final Set<String> SUPPORTED_ALGORITHMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            YundocCryptoAlgorithms.HMAC_SHA256,
            YundocCryptoAlgorithms.HMAC_SM3)));

    private final ClientSecretDigestProperties properties;
    private final SecretGenerator secretGenerator;
    private final YundocCryptoService cryptoService;

    public ClientSecretDigestService(
            ClientSecretDigestProperties properties,
            SecretGenerator secretGenerator,
            YundocCryptoService cryptoService) {
        this.properties = properties;
        this.secretGenerator = secretGenerator;
        this.cryptoService = cryptoService;
    }

    public ClientSecretDigest digestNew(String clientSecret) {
        String salt = secretGenerator.generateSalt();
        String digest = digest(clientSecret, salt, properties.getAlgorithm());
        return new ClientSecretDigest(digest, salt, properties.getAlgorithm());
    }

    public boolean matches(String rawSecret, String salt, String algorithm, String expectedDigest) {
        String actualDigest = digest(rawSecret, salt, algorithm);
        return cryptoService.matchesText(actualDigest, expectedDigest);
    }

    public String digest(String rawSecret, String salt, String algorithm) {
        String normalized = supportedAlgorithm(algorithm);
        return cryptoService.hmacHex(normalized, properties.getPepper(), rawSecret + ":" + salt);
    }

    private String supportedAlgorithm(String algorithm) {
        return requireSupportedAlgorithm(normalizedAlgorithm(algorithm));
    }

    private String normalizedAlgorithm(String algorithm) {
        if (!Texts.hasText(algorithm)) {
            throw unsupportedAlgorithm();
        }
        return algorithm.trim().toUpperCase(Locale.ROOT);
    }

    private String requireSupportedAlgorithm(String normalized) {
        if (SUPPORTED_ALGORITHMS.contains(normalized)) {
            return normalized;
        }
        throw unsupportedAlgorithm();
    }

    private YundocException unsupportedAlgorithm() {
        return new YundocException(YundocErrorCode.VALIDATION_FAILED, "Unsupported secret digest algorithm");
    }
}
