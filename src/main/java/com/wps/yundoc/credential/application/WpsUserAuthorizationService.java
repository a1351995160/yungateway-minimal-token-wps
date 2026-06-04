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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WpsUserAuthorizationService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class WpsUserAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsUserAuthorizationService.class);

    private static final int STATE_PREFIX_LENGTH = 8;

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
        WpsUserToken token = tokenCache.get(userId).orElseThrow(() -> reauthRequired(userId, businessSystemId, null));
        LOGGER.info("WPS用户凭证缓存命中 用户ID={} 业务系统ID={} 过期时间={}",
                userId,
                businessSystemId,
                token.getExpiresAt());
        return token;
    }

    public WpsUserToken requireUserToken(String userId, String businessSystemId, String clientId) {
        WpsUserToken token = tokenCache.get(userId)
                .orElseThrow(() -> reauthRequired(userId, businessSystemId, clientId));
        if (!shouldRefresh(token)) {
            LOGGER.info("WPS用户凭证缓存命中 用户ID={} 业务系统ID={} 客户端ID={} 过期时间={}",
                    userId,
                    businessSystemId,
                    clientId,
                    token.getExpiresAt());
            return token;
        }
        LOGGER.info("WPS用户凭证需要刷新 用户ID={} 业务系统ID={} 客户端ID={} 过期时间={}",
                userId,
                businessSystemId,
                clientId,
                token.getExpiresAt());
        return refreshUserToken(userId, businessSystemId, clientId, token);
    }

    public WpsAuthorizationLink authorizationLink(String userId, String businessSystemId, String clientId) {
        LOGGER.info("开始生成WPS用户授权链接 用户ID={} 业务系统ID={} 客户端ID={}",
                userId,
                businessSystemId,
                clientId);
        return createAuthorizationLink(userId, businessSystemId, clientId);
    }

    public WpsOauthCallbackResult handleCallback(String code, String stateValue) {
        LOGGER.info("开始处理WPS授权回调 状态前缀={}", statePrefix(stateValue));
        return completeAuthorization(code, stateValue);
    }

    public WpsOauthCallbackResult completeAuthorization(String code, String stateValue) {
        String validCode = requiredText(code);
        String validState = requiredText(stateValue);
        OauthState state = validState(validState);
        LOGGER.info("WPS授权码换取用户凭证开始 用户ID={} 业务系统ID={} 客户端ID={}",
                state.getUserId(),
                state.getBusinessSystemId(),
                state.getClientId());
        WpsUserToken token = authorizationClient.exchangeCode(validCode);
        tokenCache.put(state.getUserId(), token);
        LOGGER.info("WPS用户授权完成 用户ID={} 业务系统ID={} 客户端ID={} 过期时间={}",
                state.getUserId(),
                state.getBusinessSystemId(),
                state.getClientId(),
                token.getExpiresAt());
        return new WpsOauthCallbackResult(state.getUserId());
    }

    private synchronized WpsUserToken refreshUserToken(
            String userId,
            String businessSystemId,
            String clientId,
            WpsUserToken token) {
        WpsUserToken currentToken = currentRefreshCandidate(userId, token);
        if (hasFreshConcurrentRefresh(currentToken, token)) {
            LOGGER.info("WPS用户凭证已被并发刷新 用户ID={} 业务系统ID={} 客户端ID={}",
                    userId,
                    businessSystemId,
                    clientId);
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
            return refreshCurrentTokenOrThrow(userId, businessSystemId, clientId, currentToken);
        } catch (YundocException ex) {
            removeFailedToken(userId, businessSystemId, clientId, currentToken, ex);
            throw reauthRequired(userId, businessSystemId, clientId);
        }
    }

    private WpsUserToken refreshCurrentTokenOrThrow(
            String userId,
            String businessSystemId,
            String clientId,
            WpsUserToken currentToken) {
        LOGGER.info("开始刷新WPS用户凭证 用户ID={} 业务系统ID={} 客户端ID={}",
                userId,
                businessSystemId,
                clientId);
        WpsUserToken refreshed = authorizationClient.refreshToken(currentToken.getRefreshToken());
        tokenCache.put(userId, refreshed);
        LOGGER.info("WPS用户凭证刷新完成 用户ID={} 业务系统ID={} 客户端ID={} 过期时间={}",
                userId,
                businessSystemId,
                clientId,
                refreshed.getExpiresAt());
        return refreshed;
    }

    private void removeFailedToken(
            String userId,
            String businessSystemId,
            String clientId,
            WpsUserToken currentToken,
            YundocException ex) {
        tokenCache.remove(userId, currentToken);
        LOGGER.warn("WPS用户凭证刷新失败 用户ID={} 业务系统ID={} 客户端ID={} 错误码={}",
                userId,
                businessSystemId,
                clientId,
                ex.getErrorCode());
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
                .orElseThrow(() -> {
                    LOGGER.warn("WPS授权回调状态无效 状态前缀={}", statePrefix(stateValue));
                    return new YundocException(YundocErrorCode.VALIDATION_FAILED);
                });
    }

    private YundocException reauthRequired(String userId, String businessSystemId, String clientId) {
        LOGGER.info("WPS用户需要重新授权 用户ID={} 业务系统ID={} 客户端ID={}",
                userId,
                businessSystemId,
                clientId);
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
        LOGGER.info("WPS用户授权链接已生成 用户ID={} 业务系统ID={} 客户端ID={} 状态前缀={} 过期时间={}",
                userId,
                businessSystemId,
                clientId,
                statePrefix(state),
                expiresAt);
        return new WpsAuthorizationLink(
                authorizationClient.authorizeUrl(state),
                properties.getStateTtl().getSeconds());
    }

    private String statePrefix(String state) {
        if (state == null || state.length() <= STATE_PREFIX_LENGTH) {
            return state;
        }
        return state.substring(0, STATE_PREFIX_LENGTH);
    }
}
