package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * WPS request signature helper.
 *
 * @author WPS
 */
public class WpsRequestSigner {

    public static final String KSO_1 = "KSO-1";
    public static final String WPS_3 = "WPS-3";
    public static final String NONE = "NONE";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String KSO_DATE_HEADER = "X-Kso-Date";
    public static final String KSO_AUTHORIZATION_HEADER = "X-Kso-Authorization";
    public static final String WPS3_DATE_HEADER = "Date";
    public static final String WPS3_CONTENT_MD5_HEADER = "Content-Md5";
    public static final String WPS3_AUTHORIZATION_HEADER = "Authorization";

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String SHA256 = "SHA-256";
    private static final String MD5 = "MD5";
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final DateTimeFormatter WPS_RFC1123_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            .withZone(ZoneOffset.UTC);

    private final String accessKey;
    private final String secretKey;
    private final Clock clock;

    public WpsRequestSigner(String accessKey, String secretKey) {
        this(accessKey, secretKey, Clock.systemUTC());
    }

    public WpsRequestSigner(String accessKey, String secretKey, Clock clock) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.clock = clock;
    }

    public static WpsRequestSigner fromProperties(WpsClientProperties properties) {
        return new WpsRequestSigner(properties.getAppId(), properties.getAppSecret());
    }

    public WpsSignatureHeaders sign(
            String signatureVersion,
            String method,
            String requestUri,
            String contentType,
            byte[] requestBody) {
        String version = normalizedVersion(signatureVersion);
        if (NONE.equals(version)) {
            return new WpsSignatureHeaders(new LinkedHashMap<String, String>());
        }
        requireCredentials();
        String date = rfc1123Now();
        if (KSO_1.equals(version)) {
            return kso1Sign(method, requestUri, contentType, date, requestBody);
        }
        throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "Unsupported WPS signature version");
    }

    public WpsSignatureHeaders kso1Sign(
            String method,
            String requestUri,
            String contentType,
            String ksoDate,
            byte[] requestBody) {
        requireCredentials();
        String signature = hex(hmac(
                HMAC_SHA256,
                secretKey,
                KSO_1 + method + requestUri + contentType + ksoDate + sha256Hex(requestBody)));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(KSO_DATE_HEADER, ksoDate);
        headers.put(KSO_AUTHORIZATION_HEADER, KSO_1 + " " + accessKey + ":" + signature);
        return new WpsSignatureHeaders(headers);
    }

    public WpsSignatureHeaders wps3Sign(
            String requestUri,
            String contentType,
            String date,
            byte[] requestBody) {
        requireCredentials();
        String contentMd5 = base64(md5(requestBody));
        String signature = base64(hmac(
                HMAC_SHA1,
                secretKey,
                accessKey + contentMd5 + requestUri + contentType + date));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(WPS3_DATE_HEADER, date);
        headers.put(WPS3_CONTENT_MD5_HEADER, contentMd5);
        headers.put(WPS3_AUTHORIZATION_HEADER, WPS_3 + ":" + accessKey + ":" + signature);
        return new WpsSignatureHeaders(headers);
    }

    private String normalizedVersion(String signatureVersion) {
        if (!Texts.hasText(signatureVersion)) {
            return KSO_1;
        }
        return signatureVersion.trim().toUpperCase(Locale.ROOT);
    }

    private void requireCredentials() {
        if (Texts.isBlank(accessKey) || Texts.isBlank(secretKey)) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "WPS signature credentials are required");
        }
    }

    private String rfc1123Now() {
        return WPS_RFC1123_FORMATTER.format(ZonedDateTime.now(clock));
    }

    private String sha256Hex(byte[] requestBody) {
        if (requestBody == null || requestBody.length == 0) {
            return "";
        }
        return hex(digest(SHA256, requestBody));
    }

    private byte[] md5(byte[] requestBody) {
        if (requestBody == null) {
            return digest(MD5, new byte[0]);
        }
        return digest(MD5, requestBody);
    }

    private byte[] digest(String algorithm, byte[] bytes) {
        try {
            return MessageDigest.getInstance(algorithm).digest(bytes);
        } catch (GeneralSecurityException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "WPS request digest failed", ex);
        }
    }

    private byte[] hmac(String algorithm, String key, String value) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "WPS request signature failed", ex);
        }
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

    private String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
