package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsAppTokenResponse 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class WpsAppTokenResponse implements WpsEnvelope<AppTokenData> {

    private Integer code;
    private AppTokenData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public AppTokenData getData() {
        return data;
    }

    public void setData(AppTokenData data) {
        this.data = data;
    }
}
