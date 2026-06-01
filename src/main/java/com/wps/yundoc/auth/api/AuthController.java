package com.wps.yundoc.auth.api;

import com.wps.yundoc.auth.application.AuthToken;
import com.wps.yundoc.auth.application.AuthTokenRateLimiter;
import com.wps.yundoc.auth.application.AuthTokenService;
import com.wps.yundoc.auth.application.UserAssertionVerifier;
import com.wps.yundoc.businesssystem.domain.WpsIdentityType;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * AuthController component.
 *
 * @author WPS
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

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
        rateLimiter.assertAllowed(request.getClientId(), remoteAddress);
        AuthToken token = issueToken(request, httpRequest, remoteAddress);
        String requestId = RequestContextHolder.currentRequestId().orElse("unknown");
        return ApiResponse.success(new TokenResponse(token), requestId);
    }

    private AuthToken issueToken(TokenRequest request, HttpServletRequest httpRequest, String remoteAddress) {
        try {
            AuthToken token = authTokenService.issueToken(
                    request.getClientId(),
                    request.getClientSecret(),
                    request.getIdentityType(),
                    request.getUserId());
            verifyUserAssertionWhenNeeded(httpRequest, token);
            rateLimiter.recordSuccess(request.getClientId());
            return token;
        } catch (YundocException ex) {
            if (shouldRecordFailure(ex)) {
                rateLimiter.recordFailure(request.getClientId(), remoteAddress);
            }
            throw ex;
        }
    }

    private boolean shouldRecordFailure(YundocException ex) {
        YundocErrorCode errorCode = ex.getErrorCode();
        return errorCode == YundocErrorCode.TOKEN_INVALID
                || errorCode == YundocErrorCode.BUSINESS_SYSTEM_DISABLED
                || errorCode == YundocErrorCode.USER_ASSERTION_INVALID;
    }

    private void verifyUserAssertionWhenNeeded(HttpServletRequest request, AuthToken token) {
        if (token.getPrincipal().getIdentityType() != WpsIdentityType.USER) {
            return;
        }
        RequestContextHolder.set(RequestContext.builder(RequestContextHolder.currentRequestId().orElse("unknown"))
                .businessSystemId(token.getPrincipal().getBusinessSystemId())
                .clientId(token.getPrincipal().getClientId())
                .identityType(token.getPrincipal().getIdentityType())
                .jti(token.getPrincipal().getJti())
                .tokenVersion(token.getPrincipal().getTokenVersion())
                .permissionVersion(token.getPrincipal().getPermissionVersion())
                .userId(token.getPrincipal().getUserId())
                .build());
        userAssertionVerifier.verify(request, token.getPrincipal().getUserId());
    }
}
