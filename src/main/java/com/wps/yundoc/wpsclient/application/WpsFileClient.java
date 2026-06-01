package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileClient component.
 *
 * @author WPS
 */
public interface WpsFileClient {

    /**
     * Lists files from WPS for the request context.
     *
     * @param request file list request
     * @return file list
     */
    WpsFileList listFiles(WpsFileListRequest request);

    /**
     * Lists WPS drives for the current identity.
     *
     * @param request drive list request
     * @return drive list
     */
    WpsDriveList listDrives(WpsDriveListRequest request);

    /**
     * Creates a WPS drive.
     *
     * @param request create drive request
     * @return created drive
     */
    WpsDrive createDrive(WpsCreateDriveRequest request);

    /**
     * Lists child files under a WPS folder.
     *
     * @param request child file list request
     * @return child file list
     */
    WpsFileList listChildren(WpsFileChildrenRequest request);

    /**
     * Creates a WPS folder.
     *
     * @param request create folder request
     * @return created folder
     */
    WpsFileItem createFolder(WpsCreateFolderRequest request);

    /**
     * Requests WPS upload information.
     *
     * @param request upload request
     * @return upload information
     */
    WpsUploadInfo requestUpload(WpsRequestUploadRequest request);

    /**
     * Uploads entity bytes to the WPS storage URL.
     *
     * @param request entity upload request
     */
    void uploadFile(WpsUploadFileRequest request);

    /**
     * Commits a WPS upload.
     *
     * @param request commit upload request
     * @return committed WPS file
     */
    WpsFileItem commitUpload(WpsCommitUploadRequest request);
}
