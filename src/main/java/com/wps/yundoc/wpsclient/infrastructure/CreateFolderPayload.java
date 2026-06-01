package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CreateFolderPayload component.
 *
 * @author WPS
 */
class CreateFolderPayload {

    @JsonProperty("file_type")
    private final String fileType = "folder";
    private final String name;
    @JsonProperty("on_name_conflict")
    private final String onNameConflict;

    CreateFolderPayload(String name, String onNameConflict) {
        this.name = name;
        this.onNameConflict = onNameConflict;
    }

    public String getFileType() {
        return fileType;
    }

    public String getName() {
        return name;
    }

    public String getOnNameConflict() {
        return onNameConflict;
    }
}
