package com.wps.yundoc.businesssystem.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * ApiCode 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public enum ApiCode {
    /**
     * 创建应用预览链接。
     */
    APP_PREVIEW_CREATE("app-preview:create", WpsIdentityType.APP),
    /**
     * 查询用户文件列表。
     */
    USER_FILES_LIST("user-files:list", WpsIdentityType.USER),
    /**
     * 重命名用户文件。
     */
    USER_FILES_RENAME("user-files:rename", WpsIdentityType.USER),
    /**
     * 下载用户文件。
     */
    USER_FILES_DOWNLOAD("user-files:download", WpsIdentityType.USER),
    /**
     * 重命名用户文件夹。
     */
    USER_FOLDERS_RENAME("user-folders:rename", WpsIdentityType.USER),
    /**
     * 创建用户文件。
     */
    USER_FILES_CREATE("user-files:create", WpsIdentityType.USER),
    /**
     * 将用户文件另存为新文件。
     */
    USER_FILES_SAVE_AS("user-files:save-as", WpsIdentityType.USER),
    /**
     * 查看用户文件。
     */
    USER_FILES_VIEW("user-files:view", WpsIdentityType.USER),
    /**
     * 删除用户文件。
     */
    USER_FILES_DELETE("user-files:delete", WpsIdentityType.USER),
    /**
     * 更新用户文件。
     */
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
