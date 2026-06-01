package com.wps.yundoc.wpsclient.application;

import java.util.List;

/**
 * WpsDriveList component.
 *
 * @author WPS
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
