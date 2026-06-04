package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.capability.userfile.application.UserFileUploadResult;

/**
 * UserFileUploadResponse 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileUploadResponse extends UserFileItemResponse {

    public UserFileUploadResponse(UserFileUploadResult result) {
        super(result.getFile());
    }
}

