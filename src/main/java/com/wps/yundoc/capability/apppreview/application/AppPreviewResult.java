package com.wps.yundoc.capability.apppreview.application;

import java.time.OffsetDateTime;

public class AppPreviewResult {

    private final String previewUrl;
    private final OffsetDateTime expireAt;

    public AppPreviewResult(String previewUrl, OffsetDateTime expireAt) {
        this.previewUrl = previewUrl;
        this.expireAt = expireAt;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }
}
