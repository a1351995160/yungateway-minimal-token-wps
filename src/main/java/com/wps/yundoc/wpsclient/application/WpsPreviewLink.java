package com.wps.yundoc.wpsclient.application;

import java.time.OffsetDateTime;

/**
 * WpsPreviewLink 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsPreviewLink {

    private final String previewUrl;
    private final OffsetDateTime expireAt;

    public WpsPreviewLink(String previewUrl, OffsetDateTime expireAt) {
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
