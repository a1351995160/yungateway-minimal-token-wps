package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DriveData component.
 *
 * @author WPS
 */
class DriveData {

    @JsonAlias("id")
    @JsonProperty("drive_id")
    private String driveId;
    private String name;
    private String status;
    private String source;

    String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
