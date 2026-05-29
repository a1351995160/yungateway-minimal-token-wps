package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;

import java.util.List;

/**
 * UserFileListResult component.
 *
 * @author WPS
 */
public class UserFileListResult {

    private final List<WpsFileItem> items;
    private final String nextCursor;

    public UserFileListResult(WpsFileList fileList) {
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
