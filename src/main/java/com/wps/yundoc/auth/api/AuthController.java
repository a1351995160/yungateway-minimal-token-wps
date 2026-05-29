package com.wps.yundoc.auth.api;

import com.wps.yundoc.auth.application.AuthToken;
import com.wps.yundoc.auth.application.AuthTokenService;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthTokenService authTokenService;

    public AuthController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> token(@Valid @RequestBody TokenRequest request) {
        AuthToken token = authTokenService.issueToken(request.getClientId(), request.getClientSecret());
        String requestId = RequestContextHolder.currentRequestId().orElse("unknown");
        return ApiResponse.success(new TokenResponse(token), requestId);
    }
}
