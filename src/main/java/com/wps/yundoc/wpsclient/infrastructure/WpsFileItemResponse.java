package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsFileItemResponse component.
 *
 * @author WPS
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
