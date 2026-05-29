package com.wps.yundoc.capability.apppreview.application;

public class AppPreviewCommand {

    private final String sourceType;
    private final String fileId;
    private final int expireSeconds;

    public AppPreviewCommand(String sourceType, String fileId, int expireSeconds) {
        this.sourceType = sourceType;
        this.fileId = fileId;
        this.expireSeconds = expireSeconds;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getFileId() {
        return fileId;
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }
}
