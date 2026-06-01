package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * FileListItemData component.
 *
 * @author WPS
 */
public class FileListItemData {

    @JsonAlias("id")
    private String fileId;
    private String name;
    private String type;
    private boolean folder;
    @JsonAlias({"mtime", "updated_at"})
    private String updatedAt;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isFolder() {
        return folder || "folder".equals(type);
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
