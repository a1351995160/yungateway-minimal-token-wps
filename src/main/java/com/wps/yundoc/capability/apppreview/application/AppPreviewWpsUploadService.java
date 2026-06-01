package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.apppreview.domain.AppPreviewFolder;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadHash;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * AppPreviewWpsUploadService component.
 *
 * @author WPS
 */
@Service
public class AppPreviewWpsUploadService {

    private final WpsPreviewClient previewClient;
    private final WpsFileClient fileClient;
    private final AppPreviewUploadProperties uploadProperties;

    public AppPreviewWpsUploadService(
            WpsPreviewClient previewClient,
            WpsFileClient fileClient,
            AppPreviewUploadProperties uploadProperties) {
        this.previewClient = previewClient;
        this.fileClient = fileClient;
        this.uploadProperties = uploadProperties;
    }

    public AppPreviewResult uploadAndCreatePreview(
            AppPreviewCommand command,
            WpsCredential credential,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile) {
        WpsFileItem uploadedFile = uploadFile(credential, folder, stagedFile);
        WpsPreviewLink link = previewClient.createPreview(previewRequest(command, credential, uploadedFile));
        return new AppPreviewResult(link.getPreviewUrl(), link.getExpireAt(), uploadedFile.getFileId());
    }

    private WpsFileItem uploadFile(
            WpsCredential credential,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile) {
        WpsUploadInfo uploadInfo = fileClient.requestUpload(requestUploadRequest(credential, folder, stagedFile));
        fileClient.uploadFile(new WpsUploadFileRequest(
                credential.getAccessToken(),
                uploadInfo.getStoreRequest(),
                stagedFile.getPath(),
                stagedFile.getSize(),
                stagedFile.getSha256()));
        return fileClient.commitUpload(new WpsCommitUploadRequest(
                credential.getAccessToken(),
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId()));
    }

    private WpsRequestUploadRequest requestUploadRequest(
            WpsCredential credential,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile) {
        return new WpsRequestUploadRequest.Builder()
                .accessToken(credential.getAccessToken())
                .driveId(folder.getDriveId())
                .parentFileId(folder.getFolderId())
                .name(stagedFile.getFileName())
                .size(stagedFile.getSize())
                .hashes(Collections.singletonList(new WpsUploadHash("sha256", stagedFile.getSha256())))
                .internal(uploadProperties.isUploadInternal())
                .onNameConflict(uploadProperties.getUploadConflictBehavior())
                .build();
    }

    private WpsPreviewRequest previewRequest(
            AppPreviewCommand command,
            WpsCredential credential,
            WpsFileItem uploadedFile) {
        return new WpsPreviewRequest(
                uploadedFile.getFileId(),
                command.getExpireSeconds(),
                credential.getAccessToken());
    }
}
