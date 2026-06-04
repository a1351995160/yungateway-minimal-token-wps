package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.capability.upload.application.FileStagingService;
import com.wps.yundoc.capability.upload.application.StagedUploadFile;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.LogSanitizer;
import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.credential.application.WpsUserAuthorizationService;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileDownloadInfo;
import com.wps.yundoc.wpsclient.application.WpsFileDownloadRequest;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileSearchRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadHash;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;
import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * UserFileService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class UserFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserFileService.class);

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final String DEFAULT_PARENT_FILE_ID = "root";

    private final WpsUserAuthorizationService authorizationService;
    private final WpsFileClient fileClient;
    private final FileStagingService stagingService;
    private final AppPreviewUploadProperties uploadProperties;

    public UserFileService(
            WpsUserAuthorizationService authorizationService,
            WpsFileClient fileClient,
            FileStagingService stagingService,
            AppPreviewUploadProperties uploadProperties) {
        this.authorizationService = authorizationService;
        this.fileClient = fileClient;
        this.stagingService = stagingService;
        this.uploadProperties = uploadProperties;
    }

    public UserFileListResult listFiles(UserFileListCommand command) {
        long startedAt = System.nanoTime();
        logListStarted(command);
        validateUserId(command.getUserId());
        WpsUserToken token = authorizationService.requireUserToken(
                command.getUserId(),
                command.getBusinessSystemId(),
                command.getClientId());
        WpsFileList fileList = fileClient.listFiles(wpsRequest(command, token));
        logListCompleted(command, fileList, elapsedMillis(startedAt));
        return new UserFileListResult(fileList);
    }

    public UserFileSearchResult searchFiles(UserFileSearchCommand command) {
        long startedAt = System.nanoTime();
        logSearchStarted(command);
        validateUserId(command.getUserId());
        WpsUserToken token = userToken(command.getUserId(), command.getBusinessSystemId(), command.getClientId());
        WpsFileList fileList = fileClient.searchFiles(new WpsFileSearchRequest(
                token.getAccessToken(),
                command.getKeyword(),
                limit(command.getLimit()),
                command.getCursor()));
        logSearchCompleted(command, fileList, elapsedMillis(startedAt));
        return new UserFileSearchResult(fileList);
    }

    public UserFileDownloadResult downloadInfo(UserFileDownloadCommand command) {
        long startedAt = System.nanoTime();
        logDownloadStarted(command);
        validateUserId(command.getUserId());
        WpsUserToken token = userToken(command.getUserId(), command.getBusinessSystemId(), command.getClientId());
        WpsFileDownloadInfo downloadInfo = fileClient.downloadInfo(new WpsFileDownloadRequest(
                token.getAccessToken(),
                command.getDriveId(),
                command.getFileId(),
                true,
                false));
        validateDownloadUrl(downloadInfo.getUrl());
        logDownloadCompleted(command, downloadInfo, elapsedMillis(startedAt));
        return new UserFileDownloadResult(downloadInfo);
    }

    public UserFileUploadResult uploadFile(UserFileUploadCommand command) {
        long startedAt = System.nanoTime();
        logUploadStarted(command);
        validateUserId(command.getUserId());
        WpsUserToken token = userToken(command.getUserId(), command.getBusinessSystemId(), command.getClientId());
        try (StagedUploadFile stagedFile = stagingService.stage(
                command.getFile(),
                command.getDisplayName(),
                "user-file-",
                "用户文件")) {
            WpsFileItem uploadedFile = uploadFile(command, token, stagedFile);
            logUploadCompleted(command, uploadedFile, elapsedMillis(startedAt));
            return new UserFileUploadResult(uploadedFile);
        }
    }

    private WpsFileListRequest wpsRequest(UserFileListCommand command, WpsUserToken token) {
        return new WpsFileListRequest(
                token.getAccessToken(),
                parentFileId(command.getParentFileId()),
                limit(command.getLimit()),
                command.getCursor());
    }

    private WpsUserToken userToken(String userId, String businessSystemId, String clientId) {
        return authorizationService.requireUserToken(userId, businessSystemId, clientId);
    }

    private WpsFileItem uploadFile(UserFileUploadCommand command, WpsUserToken token, StagedUploadFile stagedFile) {
        WpsUploadInfo uploadInfo = requestUpload(command, token, stagedFile);
        uploadEntity(token, stagedFile, uploadInfo);
        return commitUpload(command, token, uploadInfo);
    }

    private WpsUploadInfo requestUpload(
            UserFileUploadCommand command,
            WpsUserToken token,
            StagedUploadFile stagedFile) {
        return fileClient.requestUpload(new WpsRequestUploadRequest.Builder()
                .accessToken(token.getAccessToken())
                .driveId(command.getDriveId())
                .parentFileId(command.getParentFileId())
                .name(stagedFile.getFileName())
                .size(stagedFile.getSize())
                .hashes(Collections.singletonList(new WpsUploadHash("sha256", stagedFile.getSha256())))
                .internal(uploadProperties.isUploadInternal())
                .onNameConflict(uploadProperties.getUploadConflictBehavior())
                .build());
    }

    private void uploadEntity(WpsUserToken token, StagedUploadFile stagedFile, WpsUploadInfo uploadInfo) {
        fileClient.uploadFile(new WpsUploadFileRequest(
                token.getAccessToken(),
                uploadInfo.getStoreRequest(),
                stagedFile.getPath(),
                stagedFile.getSize(),
                stagedFile.getSha256()));
    }

    private WpsFileItem commitUpload(UserFileUploadCommand command, WpsUserToken token, WpsUploadInfo uploadInfo) {
        return fileClient.commitUpload(new WpsCommitUploadRequest(
                token.getAccessToken(),
                command.getDriveId(),
                command.getParentFileId(),
                uploadInfo.getUploadId()));
    }

    private void validateDownloadUrl(String url) {
        URI uri = uri(url);
        if (isAllowedDownloadUri(uri)) {
            return;
        }
        LOGGER.warn("拒绝WPS下载地址 主机={} 协议={} 是否包含用户信息={} 是否包含片段={}",
                uri.getHost(),
                uri.getScheme(),
                Boolean.valueOf(uri.getUserInfo() != null),
                Boolean.valueOf(uri.getFragment() != null));
        throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    private URI uri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "WPS upstream error", ex);
        }
    }

    private boolean isAllowedDownloadUri(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null
                && uri.getUserInfo() == null
                && uri.getFragment() == null;
    }

    private void validateUserId(String userId) {
        if (Texts.hasText(userId)) {
            return;
        }
        throw new YundocException(YundocErrorCode.USER_ID_REQUIRED);
    }

    private String parentFileId(String parentFileId) {
        if (Texts.hasText(parentFileId)) {
            return parentFileId;
        }
        return DEFAULT_PARENT_FILE_ID;
    }

    private int limit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private void logListStarted(UserFileListCommand command) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("用户文件列表查询开始 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 父文件ID指纹={} 分页大小={}",
                    command.getBusinessSystemId(),
                    LogSanitizer.fingerprint(command.getClientId()),
                    LogSanitizer.fingerprint(command.getUserId()),
                    LogSanitizer.fingerprint(parentFileId(command.getParentFileId())),
                    Integer.valueOf(limit(command.getLimit())));
        }
    }

    private void logListCompleted(UserFileListCommand command, WpsFileList fileList, long elapsedMillis) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("用户文件列表查询完成 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 文件数量={} 是否有下一页={} 耗时毫秒={}",
                    command.getBusinessSystemId(),
                    LogSanitizer.fingerprint(command.getClientId()),
                    LogSanitizer.fingerprint(command.getUserId()),
                    Integer.valueOf(fileList.getItems().size()),
                    Boolean.valueOf(Texts.hasText(fileList.getNextCursor())),
                    Long.valueOf(elapsedMillis));
        }
    }

    private void logSearchStarted(UserFileSearchCommand command) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("用户文件搜索开始 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 关键词长度={} 分页大小={}",
                    command.getBusinessSystemId(),
                    LogSanitizer.fingerprint(command.getClientId()),
                    LogSanitizer.fingerprint(command.getUserId()),
                    Integer.valueOf(command.getKeyword().length()),
                    Integer.valueOf(limit(command.getLimit())));
        }
    }

    private void logSearchCompleted(UserFileSearchCommand command, WpsFileList fileList, long elapsedMillis) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("用户文件搜索完成 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 文件数量={} 是否有下一页={} 耗时毫秒={}",
                    command.getBusinessSystemId(),
                    LogSanitizer.fingerprint(command.getClientId()),
                    LogSanitizer.fingerprint(command.getUserId()),
                    Integer.valueOf(fileList.getItems().size()),
                    Boolean.valueOf(Texts.hasText(fileList.getNextCursor())),
                    Long.valueOf(elapsedMillis));
        }
    }

    private void logDownloadStarted(UserFileDownloadCommand command) {
        LOGGER.info("用户文件下载信息查询开始 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 空间ID指纹={} 文件ID指纹={}",
                command.getBusinessSystemId(),
                LogSanitizer.fingerprint(command.getClientId()),
                LogSanitizer.fingerprint(command.getUserId()),
                LogSanitizer.fingerprint(command.getDriveId()),
                LogSanitizer.fingerprint(command.getFileId()));
    }

    private void logDownloadCompleted(
            UserFileDownloadCommand command,
            WpsFileDownloadInfo downloadInfo,
            long elapsedMillis) {
        LOGGER.info("用户文件下载信息查询完成 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 哈希数量={} 耗时毫秒={}",
                command.getBusinessSystemId(),
                LogSanitizer.fingerprint(command.getClientId()),
                LogSanitizer.fingerprint(command.getUserId()),
                Integer.valueOf(downloadInfo.getHashes().size()),
                Long.valueOf(elapsedMillis));
    }

    private void logUploadStarted(UserFileUploadCommand command) {
        LOGGER.info("用户文件上传开始 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} 空间ID指纹={} 父文件ID指纹={}",
                command.getBusinessSystemId(),
                LogSanitizer.fingerprint(command.getClientId()),
                LogSanitizer.fingerprint(command.getUserId()),
                LogSanitizer.fingerprint(command.getDriveId()),
                LogSanitizer.fingerprint(command.getParentFileId()));
    }

    private void logUploadCompleted(UserFileUploadCommand command, WpsFileItem uploadedFile, long elapsedMillis) {
        LOGGER.info("用户文件上传完成 业务系统ID={} 客户端ID指纹={} 用户ID指纹={} WPS文件ID指纹={} 耗时毫秒={}",
                command.getBusinessSystemId(),
                LogSanitizer.fingerprint(command.getClientId()),
                LogSanitizer.fingerprint(command.getUserId()),
                LogSanitizer.fingerprint(uploadedFile.getFileId()),
                Long.valueOf(elapsedMillis));
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
