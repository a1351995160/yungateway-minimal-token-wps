package com.wps.yundoc.capability.apppreview.application;

import org.springframework.web.multipart.MultipartFile;

/**
 * AppPreviewCommand 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class AppPreviewCommand {

    private final String businessSystemId;
    private final MultipartFile file;
    private final String displayName;
    private final int expireSeconds;

    public AppPreviewCommand(String businessSystemId, MultipartFile file, String displayName, int expireSeconds) {
        this.businessSystemId = businessSystemId;
        this.file = file;
        this.displayName = displayName;
        this.expireSeconds = expireSeconds;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }
}
