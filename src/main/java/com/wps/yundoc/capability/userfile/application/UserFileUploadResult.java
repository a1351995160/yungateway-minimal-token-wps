package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.wpsclient.application.WpsFileItem;

/**
 * UserFileUploadResult 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileUploadResult {

    private final WpsFileItem file;

    public UserFileUploadResult(WpsFileItem file) {
        this.file = file;
    }

    public WpsFileItem getFile() {
        return file;
    }
}

