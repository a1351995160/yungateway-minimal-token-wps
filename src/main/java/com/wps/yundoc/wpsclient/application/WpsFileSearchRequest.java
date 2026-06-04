package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileSearchRequest 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class WpsFileSearchRequest {

    private final String accessToken;
    private final String keyword;
    private final int pageSize;
    private final String pageToken;

    public WpsFileSearchRequest(String accessToken, String keyword, int pageSize, String pageToken) {
        this.accessToken = accessToken;
        this.keyword = keyword;
        this.pageSize = pageSize;
        this.pageToken = pageToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getPageToken() {
        return pageToken;
    }
}

