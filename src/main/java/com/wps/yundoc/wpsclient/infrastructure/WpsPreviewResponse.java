package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsPreviewResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class WpsPreviewResponse implements WpsEnvelope<PreviewData> {

    private Integer code;
    private PreviewData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public PreviewData getData() {
        return data;
    }

    public void setData(PreviewData data) {
        this.data = data;
    }
}
