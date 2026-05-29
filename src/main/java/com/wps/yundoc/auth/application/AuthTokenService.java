package com.wps.yundoc.auth.application;

import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionPO;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemPO;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuthTokenService component.
 *
 * @author WPS
 */
@Service
public class AuthTokenService {

    private static final String ENABLED = "ENABLED";

    private final BizSystemMapper bizSystemMapper;
    private final BizSystemApiPermissionMapper permissionMapper;
    private final ClientSecretDigestService digestService;
    private final JwtService jwtService;

    public AuthTokenService(
            BizSystemMapper bizSystemMapper,
            BizSystemApiPermissionMapper permissionMapper,
            ClientSecretDigestService digestService,
            JwtService jwtService) {
        this.bizSystemMapper = bizSystemMapper;
        this.permissionMapper = permissionMapper;
        this.digestService = digestService;
        this.jwtService = jwtService;
    }

    public AuthToken issueToken(String clientId, String clientSecret) {
        BizSystemPO bizSystem = requireByClientId(clientId);
        requireEnabled(bizSystem);
        requireSecretMatch(clientSecret, bizSystem);
        BusinessSystemPrincipal principal = principal(bizSystem);
        List<String> apiPermissions = apiPermissions(bizSystem.getBusinessSystemId());
        long expiresIn = jwtTtlSeconds(bizSystem);
        return new AuthToken(
                jwtService.issue(principal, expiresIn),
                expiresIn,
                principal,
                apiPermissions);
    }

    private BizSystemPO requireByClientId(String clientId) {
        BizSystemPO bizSystem = bizSystemMapper.selectByClientId(clientId);
        if (bizSystem == null) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
        return bizSystem;
    }

    private void requireEnabled(BizSystemPO bizSystem) {
        if (!ENABLED.equals(bizSystem.getStatus())) {
            throw new YundocException(YundocErrorCode.BUSINESS_SYSTEM_DISABLED);
        }
    }

    private void requireSecretMatch(String clientSecret, BizSystemPO bizSystem) {
        boolean matched = digestService.matches(
                clientSecret,
                bizSystem.getClientSecretSalt(),
                bizSystem.getClientSecretAlg(),
                bizSystem.getClientSecretDigest());
        if (!matched) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private BusinessSystemPrincipal principal(BizSystemPO bizSystem) {
        return new BusinessSystemPrincipal(
                bizSystem.getBusinessSystemId(),
                bizSystem.getClientId(),
                UUID.randomUUID().toString(),
                bizSystem.getTokenVersion(),
                bizSystem.getPermissionVersion());
    }

    private List<String> apiPermissions(String businessSystemId) {
        return permissionMapper.selectByBusinessSystemId(businessSystemId)
                .stream()
                .map(BizSystemApiPermissionPO::getApiCode)
                .collect(Collectors.toList());
    }

    private long jwtTtlSeconds(BizSystemPO bizSystem) {
        Integer ttlSeconds = bizSystem.getJwtTtlSeconds();
        if (ttlSeconds == null) {
            return jwtService.expiresInSeconds();
        }
        return ttlSeconds.longValue();
    }
}
