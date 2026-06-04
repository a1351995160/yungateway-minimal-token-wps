package com.wps.yundoc.wpsclient.application;

/**
 * WpsStoreRequest 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsStoreRequest {

    private final String method;
    private final String url;

    public WpsStoreRequest(String method, String url) {
        this.method = method;
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
