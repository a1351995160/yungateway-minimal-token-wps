package com.wps.yundoc.wpsclient.infrastructure;

/**
 * PreviewPayload 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
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
