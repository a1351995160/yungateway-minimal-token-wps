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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * AppPreviewWpsUploadService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class AppPreviewWpsUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppPreviewWpsUploadService.class);

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
        long startedAt = System.nanoTime();
        logPreviewWorkflowStarted(command, folder, stagedFile);
        WpsFileItem uploadedFile = uploadFile(credential, folder, stagedFile);
        logPreviewLinkStarted(command, uploadedFile);
        WpsPreviewLink link = previewClient.createPreview(previewRequest(command, credential, uploadedFile));
        logPreviewWorkflowCompleted(command, uploadedFile, link, elapsedMillis(startedAt));
        return new AppPreviewResult(link.getPreviewUrl(), link.getExpireAt(), uploadedFile.getFileId());
    }

    private WpsFileItem uploadFile(
            WpsCredential credential,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile) {
        WpsUploadInfo uploadInfo = requestUploadInfo(credential, folder, stagedFile);
        uploadEntityFile(credential, folder, stagedFile, uploadInfo);
        return commitUpload(credential, folder, uploadInfo);
    }

    private WpsUploadInfo requestUploadInfo(
            WpsCredential credential,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile) {
        logRequestUploadStarted(folder, stagedFile);
        WpsUploadInfo uploadInfo = fileClient.requestUpload(requestUploadRequest(credential, folder, stagedFile));
        logRequestUploadCompleted(folder, uploadInfo);
        return uploadInfo;
    }

    private void uploadEntityFile(
            WpsCredential credential,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile,
            WpsUploadInfo uploadInfo) {
        logEntityUploadStarted(folder, stagedFile, uploadInfo);
        fileClient.uploadFile(new WpsUploadFileRequest(
                credential.getAccessToken(),
                uploadInfo.getStoreRequest(),
                stagedFile.getPath(),
                stagedFile.getSize(),
                stagedFile.getSha256()));
        logEntityUploadCompleted(folder, uploadInfo);
    }

    private WpsFileItem commitUpload(WpsCredential credential, AppPreviewFolder folder, WpsUploadInfo uploadInfo) {
        logCommitUploadStarted(folder, uploadInfo);
        WpsFileItem uploadedFile = fileClient.commitUpload(new WpsCommitUploadRequest(
                credential.getAccessToken(),
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId()));
        logCommitUploadCompleted(folder, uploadInfo, uploadedFile);
        return uploadedFile;
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

    private void logPreviewWorkflowStarted(
            AppPreviewCommand command,
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile) {
        LOGGER.info("WPS预览上传流程开始 业务系统ID={} 空间ID={} 文件夹ID={} 文件名={} 文件大小={}",
                command.getBusinessSystemId(),
                folder.getDriveId(),
                folder.getFolderId(),
                stagedFile.getFileName(),
                Long.valueOf(stagedFile.getSize()));
    }

    private void logPreviewLinkStarted(AppPreviewCommand command, WpsFileItem uploadedFile) {
        LOGGER.info("WPS预览链接申请开始 业务系统ID={} WPS文件ID={} 预览有效秒数={}",
                command.getBusinessSystemId(),
                uploadedFile.getFileId(),
                Integer.valueOf(command.getExpireSeconds()));
    }

    private void logPreviewWorkflowCompleted(
            AppPreviewCommand command,
            WpsFileItem uploadedFile,
            WpsPreviewLink link,
            long elapsedMillis) {
        LOGGER.info("WPS预览上传流程完成 业务系统ID={} WPS文件ID={} 过期时间={} 耗时毫秒={}",
                command.getBusinessSystemId(),
                uploadedFile.getFileId(),
                link.getExpireAt(),
                Long.valueOf(elapsedMillis));
    }

    private void logRequestUploadStarted(AppPreviewFolder folder, StagedAppPreviewFile stagedFile) {
        LOGGER.info("WPS申请上传信息开始 空间ID={} 文件夹ID={} 文件名={} 文件大小={}",
                folder.getDriveId(),
                folder.getFolderId(),
                stagedFile.getFileName(),
                Long.valueOf(stagedFile.getSize()));
    }

    private void logRequestUploadCompleted(AppPreviewFolder folder, WpsUploadInfo uploadInfo) {
        LOGGER.info("WPS申请上传信息完成 空间ID={} 文件夹ID={} 上传ID={}",
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId());
    }

    private void logEntityUploadStarted(
            AppPreviewFolder folder,
            StagedAppPreviewFile stagedFile,
            WpsUploadInfo uploadInfo) {
        LOGGER.info("WPS实体文件上传开始 空间ID={} 文件夹ID={} 上传ID={} 文件大小={}",
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId(),
                Long.valueOf(stagedFile.getSize()));
    }

    private void logEntityUploadCompleted(AppPreviewFolder folder, WpsUploadInfo uploadInfo) {
        LOGGER.info("WPS实体文件上传完成 空间ID={} 文件夹ID={} 上传ID={}",
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId());
    }

    private void logCommitUploadStarted(AppPreviewFolder folder, WpsUploadInfo uploadInfo) {
        LOGGER.info("WPS提交上传完成状态开始 空间ID={} 文件夹ID={} 上传ID={}",
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId());
    }

    private void logCommitUploadCompleted(
            AppPreviewFolder folder,
            WpsUploadInfo uploadInfo,
            WpsFileItem uploadedFile) {
        LOGGER.info("WPS提交上传完成状态完成 空间ID={} 文件夹ID={} 上传ID={} WPS文件ID={}",
                folder.getDriveId(),
                folder.getFolderId(),
                uploadInfo.getUploadId(),
                uploadedFile.getFileId());
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
