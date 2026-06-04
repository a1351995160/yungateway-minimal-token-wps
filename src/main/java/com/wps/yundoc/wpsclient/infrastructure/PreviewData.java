package com.wps.yundoc.wpsclient.infrastructure;

/**
 * PreviewData 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
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
