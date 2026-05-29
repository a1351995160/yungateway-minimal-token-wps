package com.wps.yundoc.wpsclient.infrastructure;

/**
 * PreviewData component.
 *
 * @author WPS
 */
class PreviewData {

    private String previewUrl;
    private String expireAt;

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        this.expireAt = expireAt;
    }
}
