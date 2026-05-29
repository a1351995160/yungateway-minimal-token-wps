package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.application.WpsCredentialService;
import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import org.springframework.stereotype.Service;

/**
 * AppPreviewService component.
 *
 * @author WPS
 */
@Service
public class AppPreviewService {

    private static final String WPS_FILE_SOURCE = "WPS_FILE";

    private final WpsCredentialService credentialService;
    private final WpsPreviewClient previewClient;

    public AppPreviewService(WpsCredentialService credentialService, WpsPreviewClient previewClient) {
        this.credentialService = credentialService;
        this.previewClient = previewClient;
    }

    public AppPreviewResult createPreview(AppPreviewCommand command) {
        validateSource(command);
        WpsCredential credential = credentialService.appCredential();
        WpsPreviewRequest request = previewRequest(command, credential);
        WpsPreviewLink link = previewClient.createPreview(request);
        return new AppPreviewResult(link.getPreviewUrl(), link.getExpireAt());
    }

    private void validateSource(AppPreviewCommand command) {
        if (WPS_FILE_SOURCE.equals(command.getSourceType())) {
            return;
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private WpsPreviewRequest previewRequest(AppPreviewCommand command, WpsCredential credential) {
        return new WpsPreviewRequest(
                command.getFileId(),
                command.getExpireSeconds(),
                credential.getAccessToken());
    }
}
