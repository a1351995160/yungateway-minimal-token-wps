package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.capability.apppreview.domain.AppPreviewFolder;
import com.wps.yundoc.credential.application.WpsCredentialService;
import com.wps.yundoc.credential.domain.WpsCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AppPreviewService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class AppPreviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppPreviewService.class);

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
        long startedAt = System.nanoTime();
        LOGGER.info("应用文件预览开始 业务系统ID={} 预览有效秒数={}",
                command.getBusinessSystemId(),
                Integer.valueOf(command.getExpireSeconds()));
        validateExpireSeconds(command.getExpireSeconds());
        try (StagedAppPreviewFile stagedFile = stagingService.stage(command.getFile(), command.getDisplayName())) {
            AppPreviewResult result = createPreview(command, stagedFile);
            LOGGER.info("应用文件预览完成 业务系统ID={} WPS文件ID={} 过期时间={} 耗时毫秒={}",
                    command.getBusinessSystemId(),
                    result.getFileId(),
                    result.getExpireAt(),
                    Long.valueOf(elapsedMillis(startedAt)));
            return result;
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

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
