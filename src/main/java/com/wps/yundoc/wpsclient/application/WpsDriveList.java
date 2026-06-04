package com.wps.yundoc.wpsclient.application;

import java.util.List;

/**
 * WpsDriveList 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsDriveList {

    private final List<WpsDrive> items;
    private final String nextPageToken;

    public WpsDriveList(List<WpsDrive> items, String nextPageToken) {
        this.items = items;
        this.nextPageToken = nextPageToken;
    }

    public List<WpsDrive> getItems() {
        return items;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }
}
