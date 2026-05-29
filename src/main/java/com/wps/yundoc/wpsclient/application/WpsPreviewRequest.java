package com.wps.yundoc.wpsclient.application;

import java.util.Objects;

public class WpsPreviewRequest {

    private final String fileId;
    private final int expireSeconds;
    private final String accessToken;

    public WpsPreviewRequest(String fileId, int expireSeconds, String accessToken) {
        this.fileId = fileId;
        this.expireSeconds = expireSeconds;
        this.accessToken = accessToken;
    }

    public String getFileId() {
        return fileId;
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WpsPreviewRequest)) {
            return false;
        }
        WpsPreviewRequest that = (WpsPreviewRequest) other;
        return expireSeconds == that.expireSeconds
                && Objects.equals(fileId, that.fileId)
                && Objects.equals(accessToken, that.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, expireSeconds, accessToken);
    }
}
