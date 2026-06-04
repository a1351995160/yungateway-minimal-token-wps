package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsFileItemResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class WpsFileItemResponse implements WpsEnvelope<FileListItemData> {

    private Integer code;
    private FileListItemData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public FileListItemData getData() {
        return data;
    }

    public void setData(FileListItemData data) {
        this.data = data;
    }
}
