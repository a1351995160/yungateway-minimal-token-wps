package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileChildrenRequest component.
 *
 * @author WPS
 */
public class WpsFileChildrenRequest {

    private final String accessToken;
    private final String driveId;
    private final String parentFileId;
    private final int pageSize;
    private final String pageToken;

    public WpsFileChildrenRequest(
            String accessToken,
            String driveId,
            String parentFileId,
            int pageSize,
            String pageToken) {
        this.accessToken = accessToken;
        this.driveId = driveId;
        this.parentFileId = parentFileId;
        this.pageSize = pageSize;
        this.pageToken = pageToken;
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

    public int getPageSize() {
        return pageSize;
    }

    public String getPageToken() {
        return pageToken;
    }
}
