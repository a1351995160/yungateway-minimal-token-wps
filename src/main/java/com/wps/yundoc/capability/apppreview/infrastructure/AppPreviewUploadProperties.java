package com.wps.yundoc.capability.apppreview.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AppPreviewUploadProperties component.
 *
 * @author WPS
 */
@ConfigurationProperties(prefix = "yundoc.app-preview-upload")
public class AppPreviewUploadProperties {

    private long maxFileSizeBytes = 50L * 1024L * 1024L;
    private int maxFileNameLength = 128;
    private String tempDirectory = "";
    private List<String> allowedExtensions = new ArrayList<>(Arrays.asList(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "wps", "et", "dps"));
    private String driveId = "";
    private String driveName = "YunDoc Preview";
    private String driveSource = "yundoc";
    private Long driveTotalQuota;
    private boolean autoCreateDrive;
    private String rootParentId = "0";
    private String folderNamePrefix = "yundoc-preview-";
    private int drivePageSize = 100;
    private int fileListPageSize = 100;
    private boolean uploadInternal;
    private String uploadConflictBehavior = "rename";
    private String folderConflictBehavior = "fail";

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxFileNameLength() {
        return maxFileNameLength;
    }

    public void setMaxFileNameLength(int maxFileNameLength) {
        this.maxFileNameLength = maxFileNameLength;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
    }

    public String getDriveSource() {
        return driveSource;
    }

    public void setDriveSource(String driveSource) {
        this.driveSource = driveSource;
    }

    public Long getDriveTotalQuota() {
        return driveTotalQuota;
    }

    public void setDriveTotalQuota(Long driveTotalQuota) {
        this.driveTotalQuota = driveTotalQuota;
    }

    public boolean isAutoCreateDrive() {
        return autoCreateDrive;
    }

    public void setAutoCreateDrive(boolean autoCreateDrive) {
        this.autoCreateDrive = autoCreateDrive;
    }

    public String getRootParentId() {
        return rootParentId;
    }

    public void setRootParentId(String rootParentId) {
        this.rootParentId = rootParentId;
    }

    public String getFolderNamePrefix() {
        return folderNamePrefix;
    }

    public void setFolderNamePrefix(String folderNamePrefix) {
        this.folderNamePrefix = folderNamePrefix;
    }

    public int getDrivePageSize() {
        return drivePageSize;
    }

    public void setDrivePageSize(int drivePageSize) {
        this.drivePageSize = drivePageSize;
    }

    public int getFileListPageSize() {
        return fileListPageSize;
    }

    public void setFileListPageSize(int fileListPageSize) {
        this.fileListPageSize = fileListPageSize;
    }

    public boolean isUploadInternal() {
        return uploadInternal;
    }

    public void setUploadInternal(boolean uploadInternal) {
        this.uploadInternal = uploadInternal;
    }

    public String getUploadConflictBehavior() {
        return uploadConflictBehavior;
    }

    public void setUploadConflictBehavior(String uploadConflictBehavior) {
        this.uploadConflictBehavior = uploadConflictBehavior;
    }

    public String getFolderConflictBehavior() {
        return folderConflictBehavior;
    }

    public void setFolderConflictBehavior(String folderConflictBehavior) {
        this.folderConflictBehavior = folderConflictBehavior;
    }
}
