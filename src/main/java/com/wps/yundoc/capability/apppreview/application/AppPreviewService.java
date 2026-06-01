package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.capability.apppreview.domain.AppPreviewFolder;
import com.wps.yundoc.credential.application.WpsCredentialService;
import com.wps.yundoc.credential.domain.WpsCredential;
import org.springframework.stereotype.Service;

/**
 * AppPreviewService component.
 *
 * @author WPS
 */
@Service
public class AppPreviewService {

    private static final int MIN_EXPIRE_SECONDS = 60;
    private static final int MAX_EXPIRE_SECONDS = 86400;

    private final WpsCredentialService credentialService;
    private final AppPreviewFileStagingService stagingService;
    private final AppPreviewFolderService folderService;
    private final AppPreviewWpsUploadService uploadService;

    public AppPreviewService(
            WpsCredentialService credentialService,
            AppPreviewFileStagingService stagingService,
            AppPreviewFolderService folderService,
            AppPreviewWpsUploadService uploadService) {
        this.credentialService = credentialService;
        this.stagingService = stagingService;
        this.folderService = folderService;
        this.uploadService = uploadService;
    }

    public AppPreviewResult createPreview(AppPreviewCommand command) {
        validateExpireSeconds(command.getExpireSeconds());
        try (StagedAppPreviewFile stagedFile = stagingService.stage(command.getFile(), command.getDisplayName())) {
            return createPreview(command, stagedFile);
        }
    }

    private AppPreviewResult createPreview(AppPreviewCommand command, StagedAppPreviewFile stagedFile) {
        WpsCredential credential = credentialService.appCredential();
        AppPreviewFolder folder = folderService.ensureFolder(command.getBusinessSystemId(), credential.getAccessToken());
        return uploadService.uploadAndCreatePreview(command, credential, folder, stagedFile);
    }

    private void validateExpireSeconds(int expireSeconds) {
        if (expireSeconds >= MIN_EXPIRE_SECONDS && expireSeconds <= MAX_EXPIRE_SECONDS) {
            return;
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }
}
