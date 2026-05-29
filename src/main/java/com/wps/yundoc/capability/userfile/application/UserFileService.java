package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.application.WpsUserAuthorizationService;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import org.springframework.stereotype.Service;

/**
 * UserFileService component.
 *
 * @author WPS
 */
@Service
public class UserFileService {

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
        validateUserId(command.getUserId());
        WpsUserToken token = authorizationService.requireUserToken(
                command.getUserId(),
                command.getBusinessSystemId());
        WpsFileList fileList = fileClient.listFiles(wpsRequest(command, token));
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
        if (hasText(userId)) {
            return;
        }
        throw new YundocException(YundocErrorCode.USER_ID_REQUIRED);
    }

    private String parentFileId(String parentFileId) {
        if (hasText(parentFileId)) {
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

    private boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        return !value.trim().isEmpty();
    }
}
