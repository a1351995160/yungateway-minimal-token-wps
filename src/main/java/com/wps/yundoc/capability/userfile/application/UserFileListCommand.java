package com.wps.yundoc.capability.userfile.application;

public class UserFileListCommand {

    private final String userId;
    private final String businessSystemId;
    private final String parentFileId;
    private final int limit;
    private final String cursor;

    public UserFileListCommand(
            String userId,
            String businessSystemId,
            String parentFileId,
            int limit,
            String cursor) {
        this.userId = userId;
        this.businessSystemId = businessSystemId;
        this.parentFileId = parentFileId;
        this.limit = limit;
        this.cursor = cursor;
    }

    public String getUserId() {
        return userId;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
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
