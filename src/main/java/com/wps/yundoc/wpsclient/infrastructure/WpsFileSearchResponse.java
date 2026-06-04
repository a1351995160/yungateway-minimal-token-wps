package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsFileSearchResponse 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
class WpsFileSearchResponse implements WpsEnvelope<SearchFileListData> {

    private Integer code;
    private SearchFileListData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public SearchFileListData getData() {
        return data;
    }

    public void setData(SearchFileListData data) {
        this.data = data;
    }
}

