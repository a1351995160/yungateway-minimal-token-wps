package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsEnvelope 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
interface WpsEnvelope<T> {

    /**
     * 返回 WPS 响应码。
     *
     * @return WPS 响应码
     */
    Integer getCode();

    /**
     * 返回 WPS 响应数据。
     *
     * @return 响应数据
     */
    T getData();
}
