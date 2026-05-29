package com.wps.yundoc.capability.apppreview.api;

import com.wps.yundoc.capability.apppreview.application.AppPreviewCommand;
import com.wps.yundoc.capability.apppreview.application.AppPreviewResult;
import com.wps.yundoc.capability.apppreview.application.AppPreviewService;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/app")
public class AppPreviewController {

    private final AppPreviewService appPreviewService;

    public AppPreviewController(AppPreviewService appPreviewService) {
        this.appPreviewService = appPreviewService;
    }

    @PostMapping("/previews")
    public ApiResponse<AppPreviewResponse> createPreview(@Valid @RequestBody AppPreviewRequest request) {
        AppPreviewResult result = appPreviewService.createPreview(command(request));
        String requestId = RequestContextHolder.currentRequestId().orElse("unknown");
        return ApiResponse.success(new AppPreviewResponse(result), requestId);
    }

    private AppPreviewCommand command(AppPreviewRequest request) {
        AppPreviewRequest.Source source = request.getSource();
        AppPreviewRequest.Options options = request.getOptions();
        return new AppPreviewCommand(source.getType(), source.getFileId(), options.getExpireSeconds());
    }
}
