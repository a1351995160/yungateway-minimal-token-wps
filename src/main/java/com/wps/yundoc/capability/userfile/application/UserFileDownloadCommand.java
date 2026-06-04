package com.wps.yundoc.capability.userfile.application;

/**
 * UserFileDownloadCommand 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileDownloadCommand {

    private final String userId;
    private final String businessSystemId;
    private final String clientId;
    private final String driveId;
    private final String fileId;

    public UserFileDownloadCommand(
            String userId,
            String businessSystemId,
            String clientId,
            String driveId,
            String fileId) {
        this.userId = userId;
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.driveId = driveId;
        this.fileId = fileId;
    }

    public String getUserId() {
        return userId;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getFileId() {
        return fileId;
    }
}

