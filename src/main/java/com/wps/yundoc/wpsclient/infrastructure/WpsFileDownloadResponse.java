package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsFileDownloadResponse 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
class WpsFileDownloadResponse implements WpsEnvelope<DownloadInfoData> {

    private Integer code;
    private DownloadInfoData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public DownloadInfoData getData() {
        return data;
    }

    public void setData(DownloadInfoData data) {
        this.data = data;
    }
}

