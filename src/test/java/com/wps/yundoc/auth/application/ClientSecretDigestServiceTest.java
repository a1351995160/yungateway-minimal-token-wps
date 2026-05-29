package com.wps.yundoc.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClientSecretDigestServiceTest {

    @Autowired
    private ClientSecretDigestService digestService;

    @Test
    void digestCanVerifyOriginalSecretOnly() {
        ClientSecretDigest digest = digestService.digestNew("secret-one");

        assertThat(digest.getDigest()).hasSize(64);
        assertThat(digestService.matches("secret-one", digest.getSalt(), digest.getAlgorithm(), digest.getDigest()))
                .isTrue();
        assertThat(digestService.matches("secret-two", digest.getSalt(), digest.getAlgorithm(), digest.getDigest()))
                .isFalse();
    }
}
