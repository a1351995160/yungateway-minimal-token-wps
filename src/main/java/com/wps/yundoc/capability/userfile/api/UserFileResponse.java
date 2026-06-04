package com.wps.yundoc.capability.userfile.api;

/**
 * UserFileResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class UserFileResponse {

    private final String fileId;
    private final String name;

    public UserFileResponse(String fileId, String name) {
        this.fileId = fileId;
        this.name = name;
    }

    public String getFileId() {
        return fileId;
    }

    public String getName() {
        return name;
    }
}
