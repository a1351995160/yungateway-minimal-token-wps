package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.credential.application.WpsUserAuthorizationService;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public UserFileService(WpsUserAuthorizationService authorizationService, WpsFileClient fileClient) {
        this.authorizationService = authorizationService;
        this.fileClient = fileClient;
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

    private WpsFileListRequest wpsRequest(UserFileListCommand command, WpsUserToken token) {
        return new WpsFileListRequest(
                token.getAccessToken(),
                parentFileId(command.getParentFileId()),
                limit(command.getLimit()),
                command.getCursor());
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
        LOGGER.info("用户文件列表查询开始 业务系统ID={} 客户端ID={} 用户ID={} 父文件ID={} 分页大小={}",
                command.getBusinessSystemId(),
                command.getClientId(),
                command.getUserId(),
                parentFileId(command.getParentFileId()),
                Integer.valueOf(limit(command.getLimit())));
    }

    private void logListCompleted(UserFileListCommand command, WpsFileList fileList, long elapsedMillis) {
        LOGGER.info("用户文件列表查询完成 业务系统ID={} 客户端ID={} 用户ID={} 文件数量={} 是否有下一页={} 耗时毫秒={}",
                command.getBusinessSystemId(),
                command.getClientId(),
                command.getUserId(),
                Integer.valueOf(fileList.getItems().size()),
                Boolean.valueOf(Texts.hasText(fileList.getNextCursor())),
                Long.valueOf(elapsedMillis));
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
