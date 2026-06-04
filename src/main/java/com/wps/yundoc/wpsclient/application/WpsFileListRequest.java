package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileListRequest 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsFileListRequest {

    private final String accessToken;
    private final String parentFileId;
    private final int limit;
    private final String cursor;

    public WpsFileListRequest(String accessToken, String parentFileId, int limit, String cursor) {
        this.accessToken = accessToken;
        this.parentFileId = parentFileId;
        this.limit = limit;
        this.cursor = cursor;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getParentFileId() {
        return parentFileId;
    }

    public int getLimit() {
        return limit;
    }

    public String getCursor() {
        return cursor;
    }
}
