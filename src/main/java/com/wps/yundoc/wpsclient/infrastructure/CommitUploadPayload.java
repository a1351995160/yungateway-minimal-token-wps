package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CommitUploadPayload component.
 *
 * @author WPS
 */
class CommitUploadPayload {

    @JsonProperty("upload_id")
    private final String uploadId;

    CommitUploadPayload(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getUploadId() {
        return uploadId;
    }
}
