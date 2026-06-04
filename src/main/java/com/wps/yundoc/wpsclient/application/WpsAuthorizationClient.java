package com.wps.yundoc.wpsclient.application;

import com.wps.yundoc.credential.domain.WpsUserToken;

/**
 * WpsAuthorizationClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public interface WpsAuthorizationClient {

    /**
     * 根据 state 构建 WPS 授权地址。
     *
     * @param state 授权 state
     * @return 授权地址
     */
    String authorizeUrl(String state);

    /**
     * 使用授权码换取用户令牌。
     *
     * @param code 授权码
     * @return WPS 用户令牌
     */
    WpsUserToken exchangeCode(String code);

    /**
     * 刷新 WPS 用户令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 刷新后的 WPS 用户令牌
     */
    WpsUserToken refreshToken(String refreshToken);
}
