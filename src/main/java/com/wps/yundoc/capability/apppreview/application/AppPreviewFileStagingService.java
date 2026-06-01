package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * AppPreviewFileStagingService component.
 *
 * @author WPS
 */
@Service
public class AppPreviewFileStagingService {

    private static final int BUFFER_SIZE = 8192;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final AppPreviewUploadProperties properties;

    public AppPreviewFileStagingService(AppPreviewUploadProperties properties) {
        this.properties = properties;
    }

    public StagedAppPreviewFile stage(MultipartFile file, String displayName) {
        validateFile(file);
        String fileName = validatedFileName(fileName(file, displayName));
        Path path = tempFile(fileName);
        try {
            return stageToPath(file, fileName, path);
        } catch (RuntimeException ex) {
            deleteQuietly(path);
            throw ex;
        }
    }

    private StagedAppPreviewFile stageToPath(MultipartFile file, String fileName, Path path) {
        MessageDigest digest = sha256Digest();
        long size = copy(file, path, digest);
        validateSize(size);
        return new StagedAppPreviewFile(path, fileName, size, hex(digest.digest()));
    }

    private long copy(MultipartFile file, Path path, MessageDigest digest) {
        try (InputStream inputStream = file.getInputStream();
                OutputStream outputStream = Files.newOutputStream(path)) {
            return copy(inputStream, outputStream, digest);
        } catch (IOException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Failed to stage preview file", ex);
        }
    }

    private long copy(InputStream inputStream, OutputStream outputStream, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0L;
        int read = inputStream.read(buffer);
        while (read != -1) {
            total += read;
            validateSize(total);
            digest.update(buffer, 0, read);
            outputStream.write(buffer, 0, read);
            read = inputStream.read(buffer);
        }
        return total;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
        }
    }

    private String fileName(MultipartFile file, String displayName) {
        if (Texts.hasText(displayName)) {
            return displayName.trim();
        }
        return file.getOriginalFilename();
    }

    private String validatedFileName(String fileName) {
        String normalized = normalizeFileName(fileName);
        validateFileName(normalized);
        validateExtension(normalized);
        return normalized;
    }

    private String normalizeFileName(String fileName) {
        if (!Texts.hasText(fileName)) {
            throw validationFailed();
        }
        String trimmed = fileName.trim();
        validateSafeNamePart(trimmed);
        Path normalized = Paths.get(trimmed).getFileName();
        if (normalized == null) {
            throw validationFailed();
        }
        return normalized.toString();
    }

    private void validateFileName(String fileName) {
        if (fileName.length() > properties.getMaxFileNameLength()) {
            throw validationFailed();
        }
        validateSafeNamePart(fileName);
    }

    private void validateSafeNamePart(String fileName) {
        rejectWhen(fileName.contains(".."));
        rejectWhen(fileName.contains("/"));
        rejectWhen(fileName.contains("\\"));
        rejectWhen(fileName.indexOf('\0') >= 0);
    }

    private void validateExtension(String fileName) {
        if (!hasExtensionAllowlist()) {
            return;
        }
        if (!properties.getAllowedExtensions().contains(extension(fileName))) {
            throw validationFailed();
        }
    }

    private boolean hasExtensionAllowlist() {
        return properties.getAllowedExtensions() != null && !properties.getAllowedExtensions().isEmpty();
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            throw validationFailed();
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private Path tempFile(String fileName) {
        try {
            return Files.createTempFile(tempDirectory(), "app-preview-", "-" + fileName);
        } catch (IOException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "Failed to create preview temp file", ex);
        }
    }

    private Path tempDirectory() {
        if (Texts.hasText(properties.getTempDirectory())) {
            return Paths.get(properties.getTempDirectory());
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    private void validateSize(long size) {
        if (size <= 0L || size > properties.getMaxFileSizeBytes()) {
            throw validationFailed();
        }
    }

    private void rejectWhen(boolean rejected) {
        if (rejected) {
            throw validationFailed();
        }
    }

    private YundocException validationFailed() {
        return new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "SHA-256 is unavailable", ex);
        }
    }

    private String hex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            result[index * 2] = HEX[value >>> 4];
            result[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(result);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // Best effort cleanup after a validation/staging failure.
        }
    }
}
