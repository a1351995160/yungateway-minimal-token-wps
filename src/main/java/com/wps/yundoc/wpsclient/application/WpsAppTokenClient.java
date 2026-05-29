package com.wps.yundoc.wpsclient.application;

/**
 * WpsAppTokenClient component.
 *
 * @author WPS
 */
public interface WpsAppTokenClient {

    /**
     * Issues an application access token from WPS.
     *
     * @return application token
     */
    WpsAppToken issueAppToken();
}
