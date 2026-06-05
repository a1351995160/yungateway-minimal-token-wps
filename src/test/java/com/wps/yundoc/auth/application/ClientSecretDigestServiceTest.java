package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.crypto.YundocCryptoAlgorithms;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ClientSecretDigestServiceTest {

    @Autowired
    private ClientSecretDigestService digestService;

    @Test
    void digestCanVerifyOriginalSecretOnly() {
        ClientSecretDigest digest = digestService.digestNew("secret-one");

        assertThat(digest.getDigest()).hasSize(64);
        assertThat(digest.getAlgorithm()).isEqualTo(YundocCryptoAlgorithms.HMAC_SM3);
        assertThat(digestService.matches("secret-one", digest.getSalt(), digest.getAlgorithm(), digest.getDigest()))
                .isTrue();
        assertThat(digestService.matches("secret-two", digest.getSalt(), digest.getAlgorithm(), digest.getDigest()))
                .isFalse();
    }

    @Test
    void legacyHmacSha256DigestStillMatches() {
        String digest = digestService.digest("secret-one", "salt-one", YundocCryptoAlgorithms.HMAC_SHA256);

        assertThat(digest).hasSize(64);
        assertThat(digestService.matches("secret-one", "salt-one", YundocCryptoAlgorithms.HMAC_SHA256, digest))
                .isTrue();
        assertThat(digestService.matches("secret-two", "salt-one", YundocCryptoAlgorithms.HMAC_SHA256, digest))
                .isFalse();
    }

    @Test
    void rejectsUnsupportedDigestAlgorithm() {
        assertThatThrownBy(() -> digestService.digest("secret-one", "salt-one", "UNKNOWN"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.VALIDATION_FAILED);
    }
}
