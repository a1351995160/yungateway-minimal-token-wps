package com.wps.yundoc.wpsclient.application;

/**
 * WpsDriveListRequest 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
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
