package com.wps.yundoc.wpsclient.application;

/**
 * WpsStoreRequest component.
 *
 * @author WPS
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
