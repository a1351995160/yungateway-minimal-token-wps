package com.wps.yundoc.capability.apppreview.infrastructure;

import java.time.LocalDateTime;

/**
 * AppPreviewFolderPO 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class AppPreviewFolderPO {

    private String businessSystemId;
    private String driveId;
    private String folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public void setBusinessSystemId(String businessSystemId) {
        this.businessSystemId = businessSystemId;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
