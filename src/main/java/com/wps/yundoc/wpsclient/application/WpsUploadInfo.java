package com.wps.yundoc.wpsclient.application;

/**
 * WpsUploadInfo component.
 *
 * @author WPS
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
