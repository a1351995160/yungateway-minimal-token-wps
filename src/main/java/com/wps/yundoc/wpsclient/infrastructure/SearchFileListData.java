package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * SearchFileListData 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
class SearchFileListData {

    private List<SearchFileItemData> items;
    @JsonAlias("next_page_token")
    private String nextCursor;

    public List<SearchFileItemData> getItems() {
        return items;
    }

    public void setItems(List<SearchFileItemData> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}

