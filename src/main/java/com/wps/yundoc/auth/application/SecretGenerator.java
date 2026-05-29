package com.wps.yundoc.auth.application;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * SecretGenerator component.
 *
 * @author WPS
 */
@Component
public class SecretGenerator {

    private static final int ID_BYTES = 18;
    private static final int SECRET_BYTES = 32;
    private static final int SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateBusinessSystemId() {
        return "biz_" + randomToken(ID_BYTES);
    }

    public String generateClientId() {
        return "cli_" + randomToken(ID_BYTES);
    }

    public String generateClientSecret() {
        return randomToken(SECRET_BYTES);
    }

    public String generateSalt() {
        return randomToken(SALT_BYTES);
    }

    private String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
