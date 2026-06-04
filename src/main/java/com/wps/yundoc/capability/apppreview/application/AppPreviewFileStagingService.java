package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.upload.application.FileStagingService;
import com.wps.yundoc.capability.upload.application.StagedUploadFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AppPreviewFileStagingService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class AppPreviewFileStagingService {

    private final FileStagingService stagingService;

    public AppPreviewFileStagingService(FileStagingService stagingService) {
        this.stagingService = stagingService;
    }

    public StagedAppPreviewFile stage(MultipartFile file, String displayName) {
        StagedUploadFile stagedFile = stagingService.stage(file, displayName, "app-preview-", "应用预览");
        return new StagedAppPreviewFile(
                stagedFile.getPath(),
                stagedFile.getFileName(),
                stagedFile.getSize(),
                stagedFile.getSha256());
    }
}
