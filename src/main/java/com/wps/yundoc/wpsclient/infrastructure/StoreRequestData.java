package com.wps.yundoc.wpsclient.infrastructure;

/**
 * StoreRequestData component.
 *
 * @author WPS
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
