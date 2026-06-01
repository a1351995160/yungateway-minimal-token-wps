package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UploadInfoData component.
 *
 * @author WPS
 */
class UploadInfoData {

    @JsonAlias("upload_id")
    @JsonProperty("upload_id")
    private String uploadId;
    @JsonAlias("store_request")
    @JsonProperty("store_request")
    private StoreRequestData storeRequest;

    String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    StoreRequestData getStoreRequest() {
        return storeRequest;
    }

    public void setStoreRequest(StoreRequestData storeRequest) {
        this.storeRequest = storeRequest;
    }
}
