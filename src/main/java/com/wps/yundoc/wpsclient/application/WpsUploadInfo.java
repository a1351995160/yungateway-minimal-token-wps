package com.wps.yundoc.wpsclient.application;

/**
 * WpsUploadInfo 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsUploadInfo {

    private final String uploadId;
    private final WpsStoreRequest storeRequest;

    public WpsUploadInfo(String uploadId, WpsStoreRequest storeRequest) {
        this.uploadId = uploadId;
        this.storeRequest = storeRequest;
    }

    public String getUploadId() {
        return uploadId;
    }

    public WpsStoreRequest getStoreRequest() {
        return storeRequest;
    }
}
