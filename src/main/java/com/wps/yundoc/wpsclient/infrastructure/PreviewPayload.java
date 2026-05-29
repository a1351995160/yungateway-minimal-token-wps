package com.wps.yundoc.wpsclient.infrastructure;

class PreviewPayload {

    private final String fileId;
    private final int expireSeconds;

    PreviewPayload(String fileId, int expireSeconds) {
        this.fileId = fileId;
        this.expireSeconds = expireSeconds;
    }

    public String getFileId() {
        return fileId;
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }
}
