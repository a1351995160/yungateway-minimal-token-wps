package com.wps.yundoc.credential.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.OauthState;
import com.wps.yundoc.credential.domain.WpsOauthCallbackResult;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.credential.infrastructure.LocalOauthStateCache;
import com.wps.yundoc.credential.infrastructure.LocalWpsUserTokenCache;
import com.wps.yundoc.credential.infrastructure.WpsUserAuthorizationProperties;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WpsUserAuthorizationService component.
 *
 * @author WPS
 */
@Service
public class WpsUserAuthorizationService {

    private final LocalWpsUserTokenCache tokenCache;
    private final LocalOauthStateCache stateCache;
    private final WpsAuthorizationClient authorizationClient;
    private final WpsUserAuthorizationProperties properties;

    public WpsUserAuthorizationService(
            LocalWpsUserTokenCache tokenCache,
            LocalOauthStateCache stateCache,
            WpsAuthorizationClient authorizationClient,
            WpsUserAuthorizationProperties properties) {
        this.tokenCache = tokenCache;
        this.stateCache = stateCache;
        this.authorizationClient = authorizationClient;
        this.properties = properties;
    }

    public WpsUserToken requireUserToken(String userId, String businessSystemId) {
        return tokenCache.get(userId).orElseThrow(() -> reauthRequired(userId, businessSystemId));
    }

    public WpsOauthCallbackResult handleCallback(String code, String stateValue) {
        return completeAuthorization(code, stateValue);
    }

    public WpsOauthCallbackResult completeAuthorization(String code, String stateValue) {
        String validCode = requiredText(code);
        String validState = requiredText(stateValue);
        OauthState state = validState(validState);
        WpsUserToken token = authorizationClient.exchangeCode(validCode);
        tokenCache.put(state.getUserId(), token);
        return new WpsOauthCallbackResult(state.getUserId());
    }

    private String requiredText(String value) {
        if (hasText(value)) {
            return value.trim();
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private OauthState validState(String stateValue) {
        return stateCache.take(stateValue)
                .orElseThrow(() -> new YundocException(YundocErrorCode.VALIDATION_FAILED));
    }

    private YundocException reauthRequired(String userId, String businessSystemId) {
        String state = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.getStateTtl());
        stateCache.put(new OauthState(state, userId, businessSystemId, expiresAt));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("authorizeUrl", authorizationClient.authorizeUrl(state));
        details.put("expiresIn", Long.valueOf(properties.getStateTtl().getSeconds()));
        return new YundocException(YundocErrorCode.REAUTH_REQUIRED, details);
    }

    private boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        return !value.trim().isEmpty();
    }
}
