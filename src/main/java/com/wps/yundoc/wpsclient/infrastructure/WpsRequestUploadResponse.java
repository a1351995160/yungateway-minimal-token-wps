package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsRequestUploadResponse component.
 *
 * @author WPS
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
