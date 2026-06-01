package com.wps.yundoc.wpsclient.application;

/**
 * WpsUploadHash component.
 *
 * @author WPS
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
