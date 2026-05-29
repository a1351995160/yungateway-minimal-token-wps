package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsEnvelope component.
 *
 * @author WPS
 */
interface WpsEnvelope<T> {

    /**
     * Returns WPS response code.
     *
     * @return WPS response code
     */
    Integer getCode();

    /**
     * Returns WPS response payload.
     *
     * @return response payload
     */
    T getData();
}
