package com.wps.yundoc.capability.apppreview.api;

import com.wps.yundoc.capability.apppreview.application.AppPreviewResult;

/**
 * AppPreviewResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class AppPreviewResponse {

    private final String previewUrl;
    private final String expireAt;
    private final String fileId;

    public AppPreviewResponse(AppPreviewResult result) {
        this.previewUrl = result.getPreviewUrl();
        this.expireAt = result.getExpireAt().toString();
        this.fileId = result.getFileId();
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public String getFileId() {
        return fileId;
    }
}
