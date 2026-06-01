package com.wps.yundoc.wpsclient.application;

/**
 * WpsDriveListRequest component.
 *
 * @author WPS
 */
public class WpsDriveListRequest {

    private final String accessToken;
    private final int pageSize;
    private final String pageToken;

    public WpsDriveListRequest(String accessToken, int pageSize, String pageToken) {
        this.accessToken = accessToken;
        this.pageSize = pageSize;
        this.pageToken = pageToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getPageToken() {
        return pageToken;
    }
}
