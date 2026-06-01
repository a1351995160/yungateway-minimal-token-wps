package com.wps.yundoc.wpsclient.application;

/**
 * WpsCreateFolderRequest component.
 *
 * @author WPS
 */
public class WpsCreateFolderRequest {

    private final String accessToken;
    private final String driveId;
    private final String parentFileId;
    private final String name;
    private final String onNameConflict;

    public WpsCreateFolderRequest(
            String accessToken,
            String driveId,
            String parentFileId,
            String name,
            String onNameConflict) {
        this.accessToken = accessToken;
        this.driveId = driveId;
        this.parentFileId = parentFileId;
        this.name = name;
        this.onNameConflict = onNameConflict;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getParentFileId() {
        return parentFileId;
    }

    public String getName() {
        return name;
    }

    public String getOnNameConflict() {
        return onNameConflict;
    }
}
