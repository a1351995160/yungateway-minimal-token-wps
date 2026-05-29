package com.wps.yundoc.credential.api;

import com.wps.yundoc.credential.application.WpsUserAuthorizationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/wps/oauth")
public class WpsOAuthCallbackController {

    private final WpsUserAuthorizationService authorizationService;

    public WpsOAuthCallbackController(WpsUserAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String callback(@RequestParam @NotBlank String code, @RequestParam @NotBlank String state) {
        authorizationService.handleCallback(code, state);
        return "WPS authorization completed";
    }
}
