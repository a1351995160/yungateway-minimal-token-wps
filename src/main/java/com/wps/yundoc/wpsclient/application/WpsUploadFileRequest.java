package com.wps.yundoc.wpsclient.application;

import java.nio.file.Path;

/**
 * WpsUploadFileRequest 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsUploadFileRequest {

    private final String accessToken;
    private final WpsStoreRequest storeRequest;
    private final Path file;
    private final long size;
    private final String sha256;

    public WpsUploadFileRequest(
            String accessToken,
            WpsStoreRequest storeRequest,
            Path file,
            long size,
            String sha256) {
        this.accessToken = accessToken;
        this.storeRequest = storeRequest;
        this.file = file;
        this.size = size;
        this.sha256 = sha256;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public WpsStoreRequest getStoreRequest() {
        return storeRequest;
    }

    public Path getFile() {
        return file;
    }

    public long getSize() {
        return size;
    }

    public String getSha256() {
        return sha256;
    }
}
