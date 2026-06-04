package com.wps.yundoc.credential.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.credential.infrastructure.LocalWpsTokenCache;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * WpsCredentialService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class WpsCredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsCredentialService.class);

    private final LocalWpsTokenCache tokenCache;
    private final WpsAppTokenClient tokenClient;

    public WpsCredentialService(LocalWpsTokenCache tokenCache, WpsAppTokenClient tokenClient) {
        this.tokenCache = tokenCache;
        this.tokenClient = tokenClient;
    }

    public WpsCredential appCredential() {
        java.util.Optional<WpsCredential> cached = tokenCache.get();
        if (cached.isPresent()) {
            LOGGER.info("WPS应用凭证缓存命中 过期时间={}", cached.get().getExpiresAt());
            return cached.get();
        }
        LOGGER.info("WPS应用凭证缓存未命中");
        return createAppCredential();
    }

    private WpsCredential createAppCredential() {
        long startedAt = System.nanoTime();
        LOGGER.info("WPS应用凭证申请开始");
        WpsAppToken appToken = tokenClient.issueAppToken();
        if (appToken == null) {
            LOGGER.warn("WPS应用凭证申请结果为空");
            throw new YundocException(YundocErrorCode.REAUTH_REQUIRED);
        }
        WpsCredential credential = new WpsCredential(appToken.getAccessToken(), appToken.getExpiresAt());
        tokenCache.put(credential);
        LOGGER.info("WPS应用凭证已写入缓存 过期时间={} 耗时毫秒={}",
                credential.getExpiresAt(),
                Long.valueOf(elapsedMillis(startedAt)));
        return credential;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
