package com.wps.yundoc.wpsclient.application;

/**
 * WpsCreateDriveRequest component.
 *
 * @author WPS
 */
public class WpsCreateDriveRequest {

    private final String accessToken;
    private final String name;
    private final String source;
    private final Long totalQuota;

    public WpsCreateDriveRequest(String accessToken, String name, String source, Long totalQuota) {
        this.accessToken = accessToken;
        this.name = name;
        this.source = source;
        this.totalQuota = totalQuota;
    }

    public String getAccessToken() {
        return accessToken;
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
