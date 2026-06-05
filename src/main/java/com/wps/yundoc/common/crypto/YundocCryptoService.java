package com.wps.yundoc.common.crypto;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

/**
 * 网关内部加密适配层。
 *
 * @author WPS
 * @date 2026-06-05 00:00:00
 */
@Service
public class YundocCryptoService {

    private static final String JCA_HMAC_SHA256 = "HmacSHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public byte[] hmac(String algorithm, String key, String value) {
        String normalized = normalizedAlgorithm(algorithm);
        if (YundocCryptoAlgorithms.HMAC_SHA256.equals(normalized)
                || YundocCryptoAlgorithms.JWT_HS256.equals(normalized)) {
            return hmacSha256(key, value);
        }
        if (YundocCryptoAlgorithms.HMAC_SM3.equals(normalized)
                || YundocCryptoAlgorithms.JWT_HSM3.equals(normalized)) {
            return hmacSm3(key, value);
        }
        throw unsupportedAlgorithm();
    }

    public String hmacHex(String algorithm, String key, String value) {
        return hex(hmac(algorithm, key, value));
    }

    public String hmacBase64Url(String algorithm, String key, String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(algorithm, key, value));
    }

    public boolean matches(byte[] actual, byte[] expected) {
        return MessageDigest.isEqual(actual, expected);
    }

    public boolean matchesText(String actual, String expected) {
        return matches(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizedAlgorithm(String algorithm) {
        if (!Texts.hasText(algorithm)) {
            throw unsupportedAlgorithm();
        }
        return algorithm.trim().toUpperCase(Locale.ROOT);
    }

    private byte[] hmacSha256(String key, String value) {
        try {
            Mac mac = Mac.getInstance(JCA_HMAC_SHA256);
            mac.init(new SecretKeySpec(bytes(key), JCA_HMAC_SHA256));
            return mac.doFinal(bytes(value));
        } catch (GeneralSecurityException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "HMAC-SHA256 failed", ex);
        }
    }

    private byte[] hmacSm3(String key, String value) {
        HMac mac = new HMac(new SM3Digest());
        mac.init(new KeyParameter(bytes(key)));
        byte[] input = bytes(value);
        mac.update(input, 0, input.length);
        byte[] result = new byte[mac.getMacSize()];
        mac.doFinal(result, 0);
        return result;
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String hex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            result[index * 2] = HEX[value >>> 4];
            result[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(result);
    }

    private YundocException unsupportedAlgorithm() {
        return new YundocException(YundocErrorCode.VALIDATION_FAILED, "Unsupported crypto algorithm");
    }
}
