package com.wps.yundoc.auth.application;

import com.wps.yundoc.businesssystem.infrastructure.BizSystemMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemPO;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
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

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int MAX_NONCE_LENGTH = 128;
    private static final Pattern NONCE_PATTERN = Pattern.compile("^[A-Za-z0-9._:@-]+$");

    private final BizSystemMapper bizSystemMapper;
    private final UserAssertionProperties properties;
    private final UserAssertionNonceCache nonceCache;

    public UserAssertionVerifier(
            BizSystemMapper bizSystemMapper,
            UserAssertionProperties properties,
            UserAssertionNonceCache nonceCache) {
        this.bizSystemMapper = bizSystemMapper;
        this.properties = properties;
        this.nonceCache = nonceCache;
    }

    public void verify(HttpServletRequest request, String userId) {
        if (!Texts.hasText(userId)) {
            throw new YundocException(YundocErrorCode.USER_ID_REQUIRED);
        }
        RequestContext context = requestContext();
        String assertedUserId = requiredHeader(request, USER_ID_HEADER);
        if (!Objects.equals(userId, assertedUserId)) {
            throw invalid();
        }
        String timestamp = requiredHeader(request, TIMESTAMP_HEADER);
        String nonce = validNonce(requiredHeader(request, NONCE_HEADER));
        String signature = requiredHeader(request, SIGNATURE_HEADER);
        long timestampEpochSecond = validTimestamp(timestamp);
        verifySignature(publicKey(context.getBusinessSystemId()), signature, signingInput(
                request,
                assertedUserId,
                timestamp,
                nonce));
        markNonce(context.getBusinessSystemId(), nonce, timestampEpochSecond);
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
        if (nonce.length() > MAX_NONCE_LENGTH) {
            throw invalid();
        }
        if (!NONCE_PATTERN.matcher(nonce).matches()) {
            throw invalid();
        }
        return nonce;
    }

    private long validTimestamp(String timestamp) {
        try {
            long value = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            long skewSeconds = Math.max(1L, properties.getMaxClockSkew().getSeconds());
            if (Math.abs(now - value) > skewSeconds) {
                throw invalid();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private PublicKey publicKey(String businessSystemId) {
        BizSystemPO bizSystem = bizSystemMapper.selectByBusinessSystemId(businessSystemId);
        if (bizSystem == null || !Texts.hasText(bizSystem.getUserAssertionPublicKey())) {
            throw invalid();
        }
        try {
            byte[] der = Base64.getMimeDecoder().decode(normalizePem(bizSystem.getUserAssertionPublicKey()));
            return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private String normalizePem(String pem) {
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private void verifySignature(PublicKey publicKey, String encodedSignature, String signingInput) {
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getUrlDecoder().decode(encodedSignature))) {
                throw invalid();
            }
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private String signingInput(
            HttpServletRequest request,
            String userId,
            String timestamp,
            String nonce) {
        return request.getMethod() + "\n"
                + applicationPath(request) + "\n"
                + queryString(request) + "\n"
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
