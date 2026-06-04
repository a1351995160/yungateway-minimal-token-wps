package com.wps.yundoc.capability.apppreview.application;

import java.time.OffsetDateTime;

/**
 * AppPreviewResult 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class AppPreviewResult {

    private final String previewUrl;
    private final OffsetDateTime expireAt;
    private final String fileId;

    public AppPreviewResult(String previewUrl, OffsetDateTime expireAt, String fileId) {
        this.previewUrl = previewUrl;
        this.expireAt = expireAt;
        this.fileId = fileId;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }

    public String getFileId() {
        return fileId;
    }
}
