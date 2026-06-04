package com.wps.yundoc.wpsclient.application;

/**
 * WpsAppTokenClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public interface WpsAppTokenClient {

    /**
     * 从 WPS 签发应用访问令牌。
     *
     * @return 应用令牌
     */
    WpsAppToken issueAppToken();
}
