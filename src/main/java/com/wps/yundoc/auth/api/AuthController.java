package com.wps.yundoc.auth.api;

import com.wps.yundoc.auth.application.AuthToken;
import com.wps.yundoc.auth.application.AuthTokenRateLimiter;
import com.wps.yundoc.auth.application.AuthTokenService;
import com.wps.yundoc.common.api.ApiResponse;
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

    public AuthController(AuthTokenService authTokenService, AuthTokenRateLimiter rateLimiter) {
        this.authTokenService = authTokenService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> token(
            @Valid @RequestBody TokenRequest request,
            HttpServletRequest httpRequest) {
        String remoteAddress = httpRequest.getRemoteAddr();
        rateLimiter.assertAllowed(request.getClientId(), remoteAddress);
        AuthToken token = issueToken(request, remoteAddress);
        String requestId = RequestContextHolder.currentRequestId().orElse("unknown");
        return ApiResponse.success(new TokenResponse(token), requestId);
    }

    private AuthToken issueToken(TokenRequest request, String remoteAddress) {
        try {
            AuthToken token = authTokenService.issueToken(request.getClientId(), request.getClientSecret());
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
                || errorCode == YundocErrorCode.BUSINESS_SYSTEM_DISABLED;
    }
}
