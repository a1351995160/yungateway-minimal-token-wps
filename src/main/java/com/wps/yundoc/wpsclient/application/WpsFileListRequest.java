package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileListRequest component.
 *
 * @author WPS
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
