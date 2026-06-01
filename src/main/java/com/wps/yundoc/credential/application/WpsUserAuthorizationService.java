package com.wps.yundoc.credential.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.credential.domain.OauthState;
import com.wps.yundoc.credential.domain.WpsAuthorizationLink;
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
        return tokenCache.get(userId).orElseThrow(() -> reauthRequired(userId, businessSystemId, null));
    }

    public WpsUserToken requireUserToken(String userId, String businessSystemId, String clientId) {
        WpsUserToken token = tokenCache.get(userId)
                .orElseThrow(() -> reauthRequired(userId, businessSystemId, clientId));
        if (!shouldRefresh(token)) {
            return token;
        }
        return refreshUserToken(userId, businessSystemId, clientId, token);
    }

    public WpsAuthorizationLink authorizationLink(String userId, String businessSystemId, String clientId) {
        return createAuthorizationLink(userId, businessSystemId, clientId);
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

    private synchronized WpsUserToken refreshUserToken(
            String userId,
            String businessSystemId,
            String clientId,
            WpsUserToken token) {
        WpsUserToken currentToken = currentRefreshCandidate(userId, token);
        if (hasFreshConcurrentRefresh(currentToken, token)) {
            return currentToken;
        }
        return refreshCurrentToken(userId, businessSystemId, clientId, currentToken);
    }

    private WpsUserToken currentRefreshCandidate(String userId, WpsUserToken token) {
        return tokenCache.get(userId).orElse(token);
    }

    private boolean hasFreshConcurrentRefresh(WpsUserToken currentToken, WpsUserToken token) {
        return currentToken != token && !shouldRefresh(currentToken);
    }

    private WpsUserToken refreshCurrentToken(
            String userId,
            String businessSystemId,
            String clientId,
            WpsUserToken currentToken) {
        try {
            WpsUserToken refreshed = authorizationClient.refreshToken(currentToken.getRefreshToken());
            tokenCache.put(userId, refreshed);
            return refreshed;
        } catch (YundocException ex) {
            tokenCache.remove(userId, currentToken);
            throw reauthRequired(userId, businessSystemId, clientId);
        }
    }

    private boolean shouldRefresh(WpsUserToken token) {
        OffsetDateTime refreshAt = OffsetDateTime.now().plus(properties.getRefreshSkew());
        return !token.getExpiresAt().isAfter(refreshAt);
    }

    private String requiredText(String value) {
        if (Texts.hasText(value)) {
            return value.trim();
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private OauthState validState(String stateValue) {
        return stateCache.take(stateValue)
                .orElseThrow(() -> new YundocException(YundocErrorCode.VALIDATION_FAILED));
    }

    private YundocException reauthRequired(String userId, String businessSystemId, String clientId) {
        WpsAuthorizationLink link = createAuthorizationLink(userId, businessSystemId, clientId);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("authorizeUrl", link.getAuthorizeUrl());
        details.put("expiresIn", Long.valueOf(link.getExpiresIn()));
        return new YundocException(YundocErrorCode.REAUTH_REQUIRED, details);
    }

    private WpsAuthorizationLink createAuthorizationLink(String userId, String businessSystemId, String clientId) {
        String state = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.getStateTtl());
        stateCache.put(new OauthState(state, userId, businessSystemId, clientId, expiresAt));
        return new WpsAuthorizationLink(
                authorizationClient.authorizeUrl(state),
                properties.getStateTtl().getSeconds());
    }
}
