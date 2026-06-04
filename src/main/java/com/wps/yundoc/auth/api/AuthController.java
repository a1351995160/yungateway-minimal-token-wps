package com.wps.yundoc.auth.api;

import com.wps.yundoc.auth.application.AuthToken;
import com.wps.yundoc.auth.application.AuthTokenRateLimiter;
import com.wps.yundoc.auth.application.AuthTokenService;
import com.wps.yundoc.auth.application.UserAssertionVerifier;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.businesssystem.domain.WpsIdentityType;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * AuthController 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthTokenService authTokenService;
    private final AuthTokenRateLimiter rateLimiter;
    private final UserAssertionVerifier userAssertionVerifier;

    public AuthController(
            AuthTokenService authTokenService,
            AuthTokenRateLimiter rateLimiter,
            UserAssertionVerifier userAssertionVerifier) {
        this.authTokenService = authTokenService;
        this.rateLimiter = rateLimiter;
        this.userAssertionVerifier = userAssertionVerifier;
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> token(
            @Valid @RequestBody TokenRequest request,
            HttpServletRequest httpRequest) {
        String remoteAddress = httpRequest.getRemoteAddr();
        long startedAt = System.nanoTime();
        logTokenRequestStarted(request, remoteAddress);
        rateLimiter.assertAllowed(request.getClientId(), remoteAddress);
        AuthToken token = issueToken(request, httpRequest, remoteAddress);
        logTokenRequestCompleted(request, token, elapsedMillis(startedAt));
        return ApiResponse.success(new TokenResponse(token), requestId());
    }

    private AuthToken issueToken(TokenRequest request, HttpServletRequest httpRequest, String remoteAddress) {
        try {
            AuthToken token = issueToken(request);
            verifyUserAssertionWhenNeeded(httpRequest, token);
            rateLimiter.recordSuccess(request.getClientId());
            return token;
        } catch (YundocException ex) {
            recordTokenFailure(request, remoteAddress, ex);
            throw ex;
        }
    }

    private AuthToken issueToken(TokenRequest request) {
        return authTokenService.issueToken(
                request.getClientId(),
                request.getClientSecret(),
                request.getIdentityType(),
                request.getUserId());
    }

    private boolean shouldRecordFailure(YundocException ex) {
        YundocErrorCode errorCode = ex.getErrorCode();
        return errorCode == YundocErrorCode.TOKEN_INVALID
                || errorCode == YundocErrorCode.BUSINESS_SYSTEM_DISABLED
                || errorCode == YundocErrorCode.USER_ASSERTION_INVALID;
    }

    private void verifyUserAssertionWhenNeeded(HttpServletRequest request, AuthToken token) {
        BusinessSystemPrincipal principal = token.getPrincipal();
        if (principal.getIdentityType() != WpsIdentityType.USER) {
            return;
        }
        RequestContextHolder.set(requestContext(principal));
        userAssertionVerifier.verify(request, principal.getUserId());
        logUserAssertionVerified(principal);
    }

    private RequestContext requestContext(BusinessSystemPrincipal principal) {
        return RequestContext.builder(RequestContextHolder.currentRequestId().orElse("unknown"))
                .businessSystemId(principal.getBusinessSystemId())
                .clientId(principal.getClientId())
                .identityType(principal.getIdentityType())
                .jti(principal.getJti())
                .tokenVersion(principal.getTokenVersion())
                .permissionVersion(principal.getPermissionVersion())
                .userId(principal.getUserId())
                .build();
    }

    private void logUserAssertionVerified(BusinessSystemPrincipal principal) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        LOGGER.info("用户身份断言校验通过 请求ID={} 业务系统ID={} 客户端ID指纹={} 用户ID指纹={}",
                requestId(),
                principal.getBusinessSystemId(),
                LogSanitizer.fingerprint(principal.getClientId()),
                LogSanitizer.fingerprint(principal.getUserId()));
    }

    private void recordTokenFailure(TokenRequest request, String remoteAddress, YundocException ex) {
        if (shouldRecordFailure(ex)) {
            rateLimiter.recordFailure(request.getClientId(), remoteAddress);
        }
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("令牌申请失败 请求ID={} 客户端ID指纹={} 身份类型={} 错误码={}",
                    requestId(),
                    LogSanitizer.fingerprint(request.getClientId()),
                    request.getIdentityType(),
                    ex.getErrorCode());
        }
    }

    private void logTokenRequestStarted(TokenRequest request, String remoteAddress) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("令牌申请开始 请求ID={} 客户端ID指纹={} 身份类型={} 请求来源IP指纹={}",
                    requestId(),
                    LogSanitizer.fingerprint(request.getClientId()),
                    request.getIdentityType(),
                    LogSanitizer.fingerprint(remoteAddress));
        }
    }

    private void logTokenRequestCompleted(TokenRequest request, AuthToken token, long elapsedMillis) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "令牌申请完成 请求ID={} 客户端ID指纹={} 业务系统ID={} 身份类型={} 有效秒数={} 耗时毫秒={}",
                    requestId(),
                    LogSanitizer.fingerprint(request.getClientId()),
                    token.getPrincipal().getBusinessSystemId(),
                    token.getPrincipal().getIdentityType(),
                    Long.valueOf(token.getExpiresIn()),
                    Long.valueOf(elapsedMillis));
        }
    }

    private String requestId() {
        return RequestContextHolder.currentRequestId().orElse("unknown");
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
