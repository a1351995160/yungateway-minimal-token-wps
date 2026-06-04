package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;

import java.util.List;

/**
 * UserFileSearchResult 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileSearchResult {

    private final List<WpsFileItem> items;
    private final String nextCursor;

    public UserFileSearchResult(WpsFileList fileList) {
        this.items = fileList.getItems();
        this.nextCursor = fileList.getNextCursor();
    }

    public List<WpsFileItem> getItems() {
        return items;
    }

    public String getNextCursor() {
        return nextCursor;
    }
}

