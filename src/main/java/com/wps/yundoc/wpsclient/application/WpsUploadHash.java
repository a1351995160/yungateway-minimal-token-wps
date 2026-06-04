package com.wps.yundoc.wpsclient.application;

/**
 * WpsUploadHash 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsUploadHash {

    private final String type;
    private final String sum;

    public WpsUploadHash(String type, String sum) {
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
