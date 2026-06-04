package com.wps.yundoc.wpsclient.application;

import java.util.List;

/**
 * WpsFileList 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsFileList {

    private final List<WpsFileItem> items;
    private final String nextCursor;

    public WpsFileList(List<WpsFileItem> items, String nextCursor) {
        this.items = items;
        this.nextCursor = nextCursor;
    }

    public List<WpsFileItem> getItems() {
        return items;
    }

    public String getNextCursor() {
        return nextCursor;
    }
}
