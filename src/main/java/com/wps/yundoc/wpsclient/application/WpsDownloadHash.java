package com.wps.yundoc.wpsclient.application;

/**
 * WpsDownloadHash 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class WpsDownloadHash {

    private final String type;
    private final String sum;

    public WpsDownloadHash(String type, String sum) {
        this.type = type;
        this.sum = sum;
    }

    public String getType() {
        return type;
    }

    public String getSum() {
        return sum;
    }
}

