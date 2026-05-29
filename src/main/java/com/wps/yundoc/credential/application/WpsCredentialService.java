package com.wps.yundoc.credential.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.credential.infrastructure.LocalWpsTokenCache;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import org.springframework.stereotype.Service;

/**
 * WpsCredentialService component.
 *
 * @author WPS
 */
@Service
public class WpsCredentialService {

    private final LocalWpsTokenCache tokenCache;
    private final WpsAppTokenClient tokenClient;

    public WpsCredentialService(LocalWpsTokenCache tokenCache, WpsAppTokenClient tokenClient) {
        this.tokenCache = tokenCache;
        this.tokenClient = tokenClient;
    }

    public WpsCredential appCredential() {
        return tokenCache.get().orElseGet(this::createAppCredential);
    }

    private WpsCredential createAppCredential() {
        WpsAppToken appToken = tokenClient.issueAppToken();
        if (appToken == null) {
            throw new YundocException(YundocErrorCode.REAUTH_REQUIRED);
        }
        WpsCredential credential = new WpsCredential(appToken.getAccessToken(), appToken.getExpiresAt());
        tokenCache.put(credential);
        return credential;
    }
}
