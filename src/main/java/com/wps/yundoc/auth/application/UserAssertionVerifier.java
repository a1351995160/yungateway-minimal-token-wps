package com.wps.yundoc.auth.application;

import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * UserAssertionVerifier component.
 *
 * @author WPS
 */
@Service
public class UserAssertionVerifier {

    public static final String USER_ID_HEADER = "X-Yundoc-User-Id";
    public static final String TIMESTAMP_HEADER = "X-Yundoc-User-Timestamp";
    public static final String NONCE_HEADER = "X-Yundoc-User-Nonce";
    public static final String SIGNATURE_HEADER = "X-Yundoc-User-Signature";

    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final int MAX_NONCE_LENGTH = 128;
    private static final Pattern NONCE_PATTERN = Pattern.compile("^[A-Za-z0-9._:@-]+$");

    private final ClientSecretDigestProperties digestProperties;
    private final UserAssertionProperties properties;
    private final UserAssertionNonceCache nonceCache;

    public UserAssertionVerifier(
            ClientSecretDigestProperties digestProperties,
            UserAssertionProperties properties,
            UserAssertionNonceCache nonceCache) {
        this.digestProperties = digestProperties;
        this.properties = properties;
        this.nonceCache = nonceCache;
    }

    public void verify(HttpServletRequest request, String userId) {
        requireUserId(userId);
        RequestContext context = requestContext();
        String assertedUserId = requiredHeader(request, USER_ID_HEADER);
        verifyUserId(userId, assertedUserId);
        String timestamp = requiredHeader(request, TIMESTAMP_HEADER);
        String nonce = validNonce(requiredHeader(request, NONCE_HEADER));
        String signature = requiredHeader(request, SIGNATURE_HEADER);
        long timestampEpochSecond = validTimestamp(timestamp);
        verifySignature(signature, signingInput(
                request,
                context,
                assertedUserId,
                timestamp,
                nonce));
        markNonce(context.getBusinessSystemId(), nonce, timestampEpochSecond);
    }

    private void requireUserId(String userId) {
        if (!Texts.hasText(userId)) {
            throw new YundocException(YundocErrorCode.USER_ID_REQUIRED);
        }
    }

    private void verifyUserId(String userId, String assertedUserId) {
        if (!Objects.equals(userId, assertedUserId)) {
            throw invalid();
        }
    }

    private RequestContext requestContext() {
        return RequestContextHolder.current()
                .orElseThrow(() -> new YundocException(YundocErrorCode.TOKEN_INVALID));
    }

    private String requiredHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (Texts.hasText(value)) {
            return value.trim();
        }
        throw invalid();
    }

    private String validNonce(String nonce) {
        requireValidNonceLength(nonce);
        requireValidNonceFormat(nonce);
        return nonce;
    }

    private void requireValidNonceLength(String nonce) {
        if (nonce.length() > MAX_NONCE_LENGTH) {
            throw invalid();
        }
    }

    private void requireValidNonceFormat(String nonce) {
        if (!NONCE_PATTERN.matcher(nonce).matches()) {
            throw invalid();
        }
    }

    private long validTimestamp(String timestamp) {
        try {
            return validTimestampValue(Long.parseLong(timestamp));
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private long validTimestampValue(long value) {
        if (timestampInWindow(value)) {
            return value;
        }
        throw invalid();
    }

    private boolean timestampInWindow(long value) {
        long now = Instant.now().getEpochSecond();
        long skewSeconds = Math.max(1L, properties.getMaxClockSkew().getSeconds());
        return Math.abs(now - value) <= skewSeconds;
    }

    private void verifySignature(String encodedSignature, String signingInput) {
        try {
            verifySignatureBytes(decodedSignature(encodedSignature), hmac(signingInput));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private byte[] decodedSignature(String encodedSignature) {
        return Base64.getUrlDecoder().decode(encodedSignature);
    }

    private void verifySignatureBytes(byte[] actual, byte[] expected) {
        if (!MessageDigest.isEqual(actual, expected)) {
            throw invalid();
        }
    }

    private byte[] hmac(String signingInput) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
        byte[] key = digestProperties.getPepper().getBytes(StandardCharsets.UTF_8);
        mac.init(new SecretKeySpec(key, SIGNATURE_ALGORITHM));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }

    private String signingInput(
            HttpServletRequest request,
            RequestContext context,
            String userId,
            String timestamp,
            String nonce) {
        return request.getMethod() + "\n"
                + applicationPath(request) + "\n"
                + queryString(request) + "\n"
                + context.getBusinessSystemId() + "\n"
                + context.getClientId() + "\n"
                + userId + "\n"
                + timestamp + "\n"
                + nonce;
    }

    private String applicationPath(HttpServletRequest request) {
        if (Texts.hasText(request.getServletPath())) {
            return request.getServletPath();
        }
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (!Texts.hasText(contextPath)) {
            return requestUri;
        }
        return requestUri.substring(contextPath.length());
    }

    private String queryString(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return "";
        }
        return request.getQueryString();
    }

    private void markNonce(String businessSystemId, String nonce, long timestampEpochSecond) {
        long expiresAt = timestampEpochSecond + Math.max(1L, properties.getMaxClockSkew().getSeconds());
        if (!nonceCache.markUsed(businessSystemId, nonce, expiresAt)) {
            throw invalid();
        }
    }

    private YundocException invalid() {
        return new YundocException(YundocErrorCode.USER_ASSERTION_INVALID);
    }
}
