package com.wps.yundoc.capability.userfile.application;

/**
 * UserFileSearchCommand 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileSearchCommand {

    private final String userId;
    private final String businessSystemId;
    private final String clientId;
    private final String keyword;
    private final int limit;
    private final String cursor;

    private UserFileSearchCommand(Builder builder) {
        this.userId = builder.userId;
        this.businessSystemId = builder.businessSystemId;
        this.clientId = builder.clientId;
        this.keyword = builder.keyword;
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

    public String getKeyword() {
        return keyword;
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
        private String keyword;
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

        public Builder keyword(String keyword) {
            this.keyword = keyword;
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

        public UserFileSearchCommand build() {
            return new UserFileSearchCommand(this);
        }
    }
}

