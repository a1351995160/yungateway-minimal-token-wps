package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DriveListData component.
 *
 * @author WPS
 */
class DriveListData {

    private List<DriveData> items;
    @JsonAlias("next_page_token")
    @JsonProperty("next_page_token")
    private String nextPageToken;

    List<DriveData> getItems() {
        return items;
    }

    public void setItems(List<DriveData> items) {
        this.items = items;
    }

    String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
