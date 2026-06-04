package com.wps.yundoc.wpsclient.application;

/**
 * WpsDrive 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsDrive {

    private final String driveId;
    private final String name;
    private final String status;
    private final String source;

    public WpsDrive(String driveId, String name, String status, String source) {
        this.driveId = driveId;
        this.name = name;
        this.status = status;
        this.source = source;
    }

    public String getDriveId() {
        return driveId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }
}
