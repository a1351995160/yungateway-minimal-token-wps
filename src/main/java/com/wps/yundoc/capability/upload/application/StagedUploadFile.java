package com.wps.yundoc.capability.upload.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * StagedUploadFile 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class StagedUploadFile implements AutoCloseable {

    private final Path path;
    private final String fileName;
    private final long size;
    private final String sha256;

    public StagedUploadFile(Path path, String fileName, long size, String sha256) {
        this.path = path;
        this.fileName = fileName;
        this.size = size;
        this.sha256 = sha256;
    }

    public Path getPath() {
        return path;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public String getSha256() {
        return sha256;
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Failed to clean staged upload file", ex);
        }
    }
}

