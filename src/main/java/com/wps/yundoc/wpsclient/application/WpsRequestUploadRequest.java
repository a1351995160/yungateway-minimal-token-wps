package com.wps.yundoc.wpsclient.application;

import java.util.List;

/**
 * WpsRequestUploadRequest component.
 *
 * @author WPS
 */
public class WpsRequestUploadRequest {

    private final String accessToken;
    private final String driveId;
    private final String parentFileId;
    private final String name;
    private final long size;
    private final List<WpsUploadHash> hashes;
    private final boolean internal;
    private final String onNameConflict;

    public WpsRequestUploadRequest(Builder builder) {
        this.accessToken = builder.accessToken;
        this.driveId = builder.driveId;
        this.parentFileId = builder.parentFileId;
        this.name = builder.name;
        this.size = builder.size;
        this.hashes = builder.hashes;
        this.internal = builder.internal;
        this.onNameConflict = builder.onNameConflict;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getParentFileId() {
        return parentFileId;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public List<WpsUploadHash> getHashes() {
        return hashes;
    }

    public boolean isInternal() {
        return internal;
    }

    public String getOnNameConflict() {
        return onNameConflict;
    }

    public static class Builder {

        private String accessToken;
        private String driveId;
        private String parentFileId;
        private String name;
        private long size;
        private List<WpsUploadHash> hashes;
        private boolean internal;
        private String onNameConflict;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder driveId(String driveId) {
            this.driveId = driveId;
            return this;
        }

        public Builder parentFileId(String parentFileId) {
            this.parentFileId = parentFileId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder hashes(List<WpsUploadHash> hashes) {
            this.hashes = hashes;
            return this;
        }

        public Builder internal(boolean internal) {
            this.internal = internal;
            return this;
        }

        public Builder onNameConflict(String onNameConflict) {
            this.onNameConflict = onNameConflict;
            return this;
        }

        public WpsRequestUploadRequest build() {
            return new WpsRequestUploadRequest(this);
        }
    }
}
