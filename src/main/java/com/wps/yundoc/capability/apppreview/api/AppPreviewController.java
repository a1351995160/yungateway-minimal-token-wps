package com.wps.yundoc.capability.apppreview.api;

import com.wps.yundoc.capability.apppreview.application.AppPreviewCommand;
import com.wps.yundoc.capability.apppreview.application.AppPreviewResult;
import com.wps.yundoc.capability.apppreview.application.AppPreviewService;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AppPreviewController component.
 *
 * @author WPS
 */
@RestController
@RequestMapping("/api/v1/app")
public class AppPreviewController {

    private final AppPreviewService appPreviewService;

    public AppPreviewController(AppPreviewService appPreviewService) {
        this.appPreviewService = appPreviewService;
    }

    @PostMapping(value = "/previews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AppPreviewResponse> createPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "expireSeconds", required = false, defaultValue = "3600") int expireSeconds) {
        AppPreviewResult result = appPreviewService.createPreview(command(file, displayName, expireSeconds));
        String requestId = RequestContextHolder.currentRequestId().orElse("unknown");
        return ApiResponse.success(new AppPreviewResponse(result), requestId);
    }

    private AppPreviewCommand command(MultipartFile file, String displayName, int expireSeconds) {
        return new AppPreviewCommand(
                requestContext().getBusinessSystemId(),
                file,
                displayName,
                expireSeconds);
    }

    private RequestContext requestContext() {
        return RequestContextHolder.current()
                .orElseThrow(() -> new YundocException(YundocErrorCode.TOKEN_INVALID));
    }
}
