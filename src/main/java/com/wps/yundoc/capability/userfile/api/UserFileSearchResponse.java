package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.capability.userfile.application.UserFileSearchResult;
import com.wps.yundoc.wpsclient.application.WpsFileItem;

import java.util.ArrayList;
import java.util.List;

/**
 * UserFileSearchResponse 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileSearchResponse {

    private final List<UserFileItemResponse> items;
    private final String nextCursor;

    public UserFileSearchResponse(UserFileSearchResult result) {
        this.items = toItems(result.getItems());
        this.nextCursor = result.getNextCursor();
    }

    public List<UserFileItemResponse> getItems() {
        return items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    private List<UserFileItemResponse> toItems(List<WpsFileItem> items) {
        List<UserFileItemResponse> responses = new ArrayList<>();
        for (WpsFileItem item : items) {
            responses.add(new UserFileItemResponse(item));
        }
        return responses;
    }
}

