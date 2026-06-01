package com.wps.yundoc.auth.domain;

import com.wps.yundoc.businesssystem.domain.WpsIdentityType;

/**
 * BusinessSystemPrincipal component.
 *
 * @author WPS
 */
public class BusinessSystemPrincipal {

    private final String businessSystemId;
    private final String clientId;
    private final WpsIdentityType identityType;
    private final String userId;
    private final String jti;
    private final Integer tokenVersion;
    private final Integer permissionVersion;

    public BusinessSystemPrincipal(
            String businessSystemId,
            String clientId,
            String jti,
            Integer tokenVersion,
            Integer permissionVersion) {
        this.businessSystemId = businessSystemId;
        this.clientId = clientId;
        this.identityType = WpsIdentityType.APP;
        this.userId = null;
        this.jti = jti;
        this.tokenVersion = tokenVersion;
        this.permissionVersion = permissionVersion;
    }

    private BusinessSystemPrincipal(Builder builder) {
        this.businessSystemId = builder.businessSystemId;
        this.clientId = builder.clientId;
        this.identityType = builder.identityType;
        this.userId = builder.userId;
        this.jti = builder.jti;
        this.tokenVersion = builder.tokenVersion;
        this.permissionVersion = builder.permissionVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBusinessSystemId() {
        return businessSystemId;
    }

    public String getClientId() {
        return clientId;
    }

    public WpsIdentityType getIdentityType() {
        return identityType;
    }

    public String getUserId() {
        return userId;
    }

    public String getJti() {
        return jti;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public Integer getPermissionVersion() {
        return permissionVersion;
    }

    public static class Builder {

        private String businessSystemId;
        private String clientId;
        private WpsIdentityType identityType = WpsIdentityType.APP;
        private String userId;
        private String jti;
        private Integer tokenVersion;
        private Integer permissionVersion;

        public Builder businessSystemId(String businessSystemId) {
            this.businessSystemId = businessSystemId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder identityType(WpsIdentityType identityType) {
            this.identityType = identityType;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder jti(String jti) {
            this.jti = jti;
            return this;
        }

        public Builder tokenVersion(Integer tokenVersion) {
            this.tokenVersion = tokenVersion;
            return this;
        }

        public Builder permissionVersion(Integer permissionVersion) {
            this.permissionVersion = permissionVersion;
            return this;
        }

        public BusinessSystemPrincipal build() {
            return new BusinessSystemPrincipal(this);
        }
    }
}
