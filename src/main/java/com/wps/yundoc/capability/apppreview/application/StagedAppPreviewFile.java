package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.upload.application.StagedUploadFile;

import java.nio.file.Path;

/**
 * StagedAppPreviewFile 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class StagedAppPreviewFile extends StagedUploadFile {

    public StagedAppPreviewFile(Path path, String fileName, long size, String sha256) {
        super(path, fileName, size, sha256);
    }
}
