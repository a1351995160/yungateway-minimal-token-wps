package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * FileListData 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class FileListData {

    private List<FileListItemData> items;
    @JsonAlias("next_page_token")
    private String nextCursor;

    public List<FileListItemData> getItems() {
        return items;
    }

    public void setItems(List<FileListItemData> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
