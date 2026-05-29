package com.wps.yundoc.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Pagination component.
 *
 * @author WPS
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pagination {

    private final String nextCursor;
    private final Integer pageSize;
    private final Boolean hasMore;

    private Pagination(String nextCursor, Integer pageSize, Boolean hasMore) {
        this.nextCursor = nextCursor;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
    }

    public static Pagination cursor(String nextCursor, Integer pageSize, Boolean hasMore) {
        return new Pagination(nextCursor, pageSize, hasMore);
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Boolean getHasMore() {
        return hasMore;
    }
}

