package com.wps.yundoc.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 日志脱敏工具。
 *
 * @author WPS
 * @date 2026-06-04 11:14:00
 */
public final class LogSanitizer {

    private static final int FINGERPRINT_LENGTH = 12;
    private static final int HEX_CHARS_PER_BYTE = 2;
    private static final int LOW_NIBBLE_MASK = 0x0f;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private LogSanitizer() {
    }

    public static String fingerprint(String value) {
        if (!Texts.hasText(value)) {
            return "empty";
        }
        byte[] digest = sha256(value.trim());
        char[] result = new char[FINGERPRINT_LENGTH];
        for (int i = 0; i < FINGERPRINT_LENGTH / HEX_CHARS_PER_BYTE; i++) {
            int current = digest[i] & 0xff;
            int offset = i * HEX_CHARS_PER_BYTE;
            result[offset] = HEX[current >>> 4];
            result[offset + 1] = HEX[current & LOW_NIBBLE_MASK];
        }
        return new String(result);
    }

    private static byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("日志指纹计算失败", ex);
        }
    }
}
