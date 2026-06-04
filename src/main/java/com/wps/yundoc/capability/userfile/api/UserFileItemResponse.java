package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.wpsclient.application.WpsFileItem;

/**
 * UserFileItemResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class UserFileItemResponse extends UserFileResponse {

    private final WpsFileItem item;

    public UserFileItemResponse(WpsFileItem item) {
        super(item.getFileId(), item.getName());
        this.item = item;
    }

    public String getType() {
        return item.getType();
    }

    public boolean isFolder() {
        return item.isFolder();
    }

    public String getUpdatedAt() {
        return item.getUpdatedAt();
    }
}
