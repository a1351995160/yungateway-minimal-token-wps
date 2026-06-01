package com.wps.yundoc.wpsclient.application;

/**
 * WpsCommitUploadRequest component.
 *
 * @author WPS
 */
public class WpsCommitUploadRequest {

    private final String accessToken;
    private final String driveId;
    private final String parentFileId;
    private final String uploadId;

    public WpsCommitUploadRequest(String accessToken, String driveId, String parentFileId, String uploadId) {
        this.accessToken = accessToken;
        this.driveId = driveId;
        this.parentFileId = parentFileId;
        this.uploadId = uploadId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getParentFileId() {
        return parentFileId;
    }

    public String getUploadId() {
        return uploadId;
    }
}
