package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wps.yundoc.wpsclient.application.WpsUploadHash;

import java.util.List;

/**
 * RequestUploadPayload 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class RequestUploadPayload {

    private final List<WpsUploadHash> hashes;
    private final boolean internal;
    private final String name;
    @JsonProperty("on_name_conflict")
    private final String onNameConflict;
    private final long size;

    RequestUploadPayload(
            List<WpsUploadHash> hashes,
            boolean internal,
            String name,
            String onNameConflict,
            long size) {
        this.hashes = hashes;
        this.internal = internal;
        this.name = name;
        this.onNameConflict = onNameConflict;
        this.size = size;
    }

    public List<WpsUploadHash> getHashes() {
        return hashes;
    }

    public boolean isInternal() {
        return internal;
    }

    public String getName() {
        return name;
    }

    public String getOnNameConflict() {
        return onNameConflict;
    }

    public long getSize() {
        return size;
    }
}
