package com.wps.yundoc.wpsclient.application;

import java.time.OffsetDateTime;

/**
 * WpsPreviewLink component.
 *
 * @author WPS
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
