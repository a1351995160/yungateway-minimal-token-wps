package com.wps.yundoc.wpsclient.application;

import com.wps.yundoc.credential.domain.WpsUserToken;

public interface WpsAuthorizationClient {

    String authorizeUrl(String state);

    WpsUserToken exchangeCode(String code);
}
