package com.wps.yundoc.capability.userfile.application;

/**
 * UserFileListCommand component.
 *
 * @author WPS
 */
public class UserFileListCommand {

    private final String userId;
    private final String businessSystemId;
    private final String clientId;
    private final String parentFileId;
    private final int limit;
    private final String cursor;

    private UserFileListCommand(Builder builder) {
        this.userId = builder.userId;
        this.businessSystemId = builder.businessSystemId;
        this.clientId = builder.clientId;
        this.parentFileId = builder.parentFileId;
        this.limit = builder.limit;
        this.cursor = builder.cursor;
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

    public String getParentFileId() {
        return parentFileId;
    }

    public int getLimit() {
        return limit;
    }

    public String getCursor() {
        return cursor;
    }

    public static class Builder {

        private String userId;
        private String businessSystemId;
        private String clientId;
        private String parentFileId;
        private int limit;
        private String cursor;

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

        public Builder parentFileId(String parentFileId) {
            this.parentFileId = parentFileId;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder cursor(String cursor) {
            this.cursor = cursor;
            return this;
        }

        public UserFileListCommand build() {
            return new UserFileListCommand(this);
        }
    }
}
