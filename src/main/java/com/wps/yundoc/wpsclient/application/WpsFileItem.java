package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileItem component.
 *
 * @author WPS
 */
public class WpsFileItem {

    private final String fileId;
    private final String name;
    private final String type;
    private final boolean folder;
    private final String updatedAt;

    public WpsFileItem(String fileId, String name, String type, boolean folder, String updatedAt) {
        this.fileId = fileId;
        this.name = name;
        this.type = type;
        this.folder = folder;
        this.updatedAt = updatedAt;
    }

    public String getFileId() {
        return fileId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isFolder() {
        return folder;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
