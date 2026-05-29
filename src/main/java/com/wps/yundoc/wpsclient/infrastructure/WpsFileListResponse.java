package com.wps.yundoc.wpsclient.infrastructure;

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
