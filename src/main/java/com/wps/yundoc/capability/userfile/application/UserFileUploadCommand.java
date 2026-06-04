package com.wps.yundoc.capability.userfile.application;

import org.springframework.web.multipart.MultipartFile;

/**
 * UserFileUploadCommand 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileUploadCommand {

    private final String userId;
    private final String businessSystemId;
    private final String clientId;
    private final String driveId;
    private final String parentFileId;
    private final MultipartFile file;
    private final String displayName;

    private UserFileUploadCommand(Builder builder) {
        this.userId = builder.userId;
        this.businessSystemId = builder.businessSystemId;
        this.clientId = builder.clientId;
        this.driveId = builder.driveId;
        this.parentFileId = builder.parentFileId;
        this.file = builder.file;
        this.displayName = builder.displayName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUserId() {
        return userId;
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getParentFileId() {
        return parentFileId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static class Builder {

        private String userId;
        private String businessSystemId;
        private String clientId;
        private String driveId;
        private String parentFileId;
        private MultipartFile file;
        private String displayName;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder businessSystemId(String businessSystemId) {
            this.businessSystemId = businessSystemId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
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

        public Builder file(MultipartFile file) {
            this.file = file;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public UserFileUploadCommand build() {
            return new UserFileUploadCommand(this);
        }
    }
}
