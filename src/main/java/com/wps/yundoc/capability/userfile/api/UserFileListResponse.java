package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.capability.userfile.application.UserFileListResult;
import com.wps.yundoc.wpsclient.application.WpsFileItem;

import java.util.ArrayList;
import java.util.List;

/**
 * UserFileListResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class UserFileListResponse {

    private final List<UserFileItemResponse> items;
    private final String nextCursor;

    public UserFileListResponse(UserFileListResult result) {
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
