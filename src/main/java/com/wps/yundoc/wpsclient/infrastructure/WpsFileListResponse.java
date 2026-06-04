package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsFileListResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsFileListResponse implements WpsEnvelope<FileListData> {

    private Integer code;
    private FileListData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public FileListData getData() {
        return data;
    }

    public void setData(FileListData data) {
        this.data = data;
    }
}
