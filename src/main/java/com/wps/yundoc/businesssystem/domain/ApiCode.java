package com.wps.yundoc.businesssystem.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ApiCode {
    APP_PREVIEW_CREATE("app-preview:create", WpsIdentityType.APP),
    USER_FILES_LIST("user-files:list", WpsIdentityType.USER),
    USER_FILES_RENAME("user-files:rename", WpsIdentityType.USER),
    USER_FILES_DOWNLOAD("user-files:download", WpsIdentityType.USER),
    USER_FOLDERS_RENAME("user-folders:rename", WpsIdentityType.USER),
    USER_FILES_CREATE("user-files:create", WpsIdentityType.USER),
    USER_FILES_SAVE_AS("user-files:save-as", WpsIdentityType.USER),
    USER_FILES_VIEW("user-files:view", WpsIdentityType.USER),
    USER_FILES_DELETE("user-files:delete", WpsIdentityType.USER),
    USER_FILES_UPDATE("user-files:update", WpsIdentityType.USER);

    private final String code;
    private final WpsIdentityType identityType;

    ApiCode(String code, WpsIdentityType identityType) {
        this.code = code;
        this.identityType = identityType;
    }

    public String getCode() {
        return code;
    }

    public WpsIdentityType getIdentityType() {
        return identityType;
    }

    public static Optional<ApiCode> fromCode(String code) {
        return Arrays.stream(values())
                .filter(apiCode -> apiCode.code.equals(code))
                .findFirst();
    }
}
