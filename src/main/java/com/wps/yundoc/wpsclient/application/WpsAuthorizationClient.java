package com.wps.yundoc.wpsclient.application;

import com.wps.yundoc.credential.domain.WpsUserToken;

/**
 * WpsAuthorizationClient component.
 *
 * @author WPS
 */
public interface WpsAuthorizationClient {

    /**
     * Builds the WPS authorization URL for the given state.
     *
     * @param state authorization state
     * @return authorization URL
     */
    String authorizeUrl(String state);

    /**
     * Exchanges an authorization code for a user token.
     *
     * @param code authorization code
     * @return WPS user token
     */
    WpsUserToken exchangeCode(String code);

    /**
     * Refreshes a WPS user token.
     *
     * @param refreshToken refresh token
     * @return refreshed WPS user token
     */
    WpsUserToken refreshToken(String refreshToken);
}
