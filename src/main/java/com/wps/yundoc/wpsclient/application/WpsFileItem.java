package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileItem 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsFileItem {

    private final String fileId;
    private final String driveId;
    private final String name;
    private final String type;
    private final boolean folder;
    private final String updatedAt;

    public WpsFileItem(String fileId, String name, String type, boolean folder, String updatedAt) {
        this(builder()
                .fileId(fileId)
                .name(name)
                .type(type)
                .folder(folder)
                .updatedAt(updatedAt));
    }

    private WpsFileItem(Builder builder) {
        this.fileId = builder.fileId;
        this.driveId = builder.driveId;
        this.name = builder.name;
        this.type = builder.type;
        this.folder = builder.folder;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFileId() {
        return fileId;
    }

    public String getDriveId() {
        return driveId;
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

    public static class Builder {
        private String fileId;
        private String driveId;
        private String name;
        private String type;
        private boolean folder;
        private String updatedAt;

        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder driveId(String driveId) {
            this.driveId = driveId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder folder(boolean folder) {
            this.folder = folder;
            return this;
        }

        public Builder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public WpsFileItem build() {
            return new WpsFileItem(this);
        }
    }
}
