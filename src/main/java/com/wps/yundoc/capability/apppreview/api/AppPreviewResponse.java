package com.wps.yundoc.capability.apppreview.api;

import com.wps.yundoc.capability.apppreview.application.AppPreviewResult;

/**
 * AppPreviewResponse component.
 *
 * @author WPS
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
