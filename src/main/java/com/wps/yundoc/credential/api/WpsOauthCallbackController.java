package com.wps.yundoc.credential.api;

import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.WpsAuthorizationLink;
import com.wps.yundoc.credential.application.WpsUserAuthorizationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * WpsOauthCallbackController component.
 *
 * @author WPS
 */
@RestController
@RequestMapping("/api/v1/wps/oauth")
public class WpsOauthCallbackController {

    private final WpsUserAuthorizationService authorizationService;

    public WpsOauthCallbackController(WpsUserAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String callback(@RequestParam @NotBlank String code, @RequestParam @NotBlank String state) {
        authorizationService.handleCallback(code, state);
        return "WPS authorization completed";
    }

    @GetMapping("/authorize-url")
    public ApiResponse<WpsAuthorizationLinkResponse> authorizeUrl() {
        RequestContext context = requestContext();
        WpsAuthorizationLink link = authorizationService.authorizationLink(
                context.getUserId(),
                context.getBusinessSystemId(),
                context.getClientId());
        return ApiResponse.success(new WpsAuthorizationLinkResponse(link), context.getRequestId());
    }

    private RequestContext requestContext() {
        return RequestContextHolder.current()
                .orElseThrow(() -> new YundocException(YundocErrorCode.TOKEN_INVALID));
    }
}
