package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CreateFolderPayload 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class CreateFolderPayload {

    private static final String FILE_TYPE = "folder";

    private final String name;
    @JsonProperty("on_name_conflict")
    private final String onNameConflict;

    CreateFolderPayload(String name, String onNameConflict) {
        this.name = name;
        this.onNameConflict = onNameConflict;
    }

    @JsonProperty("file_type")
    public String getFileType() {
        return FILE_TYPE;
    }

    public String getName() {
        return name;
    }

    public String getOnNameConflict() {
        return onNameConflict;
    }
}
