package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileDownloadRequest 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class WpsFileDownloadRequest {

    private final String accessToken;
    private final String driveId;
    private final String fileId;
    private final boolean withHash;
    private final boolean internal;

    public WpsFileDownloadRequest(
            String accessToken,
            String driveId,
            String fileId,
            boolean withHash,
            boolean internal) {
        this.accessToken = accessToken;
        this.driveId = driveId;
        this.fileId = fileId;
        this.withHash = withHash;
        this.internal = internal;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getFileId() {
        return fileId;
    }

    public boolean isWithHash() {
        return withHash;
    }

    public boolean isInternal() {
        return internal;
    }
}

