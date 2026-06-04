package com.wps.yundoc.wpsclient.infrastructure;

/**
 * StoreRequestData 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
class StoreRequestData {

    private String method;
    private String url;

    String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
