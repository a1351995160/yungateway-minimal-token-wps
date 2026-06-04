package com.wps.yundoc.capability.apppreview.domain;

/**
 * AppPreviewFolder 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class AppPreviewFolder {

    private final String driveId;
    private final String folderId;
    private final String folderName;

    public AppPreviewFolder(String driveId, String folderId, String folderName) {
        this.driveId = driveId;
        this.folderId = folderId;
        this.folderName = folderName;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getFolderId() {
        return folderId;
    }

    public String getFolderName() {
        return folderName;
    }
}
