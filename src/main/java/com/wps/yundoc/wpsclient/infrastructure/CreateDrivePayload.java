package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CreateDrivePayload component.
 *
 * @author WPS
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class CreateDrivePayload {

    private static final String ALLOTEE_TYPE = "app";

    private final String name;
    private final String source;
    @JsonProperty("total_quota")
    private final Long totalQuota;

    CreateDrivePayload(String name, String source, Long totalQuota) {
        this.name = name;
        this.source = source;
        this.totalQuota = totalQuota;
    }

    @JsonProperty("allotee_type")
    public String getAlloteeType() {
        return ALLOTEE_TYPE;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public Long getTotalQuota() {
        return totalQuota;
    }
}
