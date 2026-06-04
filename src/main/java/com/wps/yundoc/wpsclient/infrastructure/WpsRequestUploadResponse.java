package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsRequestUploadResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class WpsRequestUploadResponse implements WpsEnvelope<UploadInfoData> {

    private Integer code;
    private UploadInfoData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public UploadInfoData getData() {
        return data;
    }

    public void setData(UploadInfoData data) {
        this.data = data;
    }
}
