package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CommitUploadPayload 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
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
