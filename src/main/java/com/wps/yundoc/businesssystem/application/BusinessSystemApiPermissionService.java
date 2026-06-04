package com.wps.yundoc.businesssystem.application;

import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionPO;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemPO;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * BusinessSystemApiPermissionService 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Service
public class BusinessSystemApiPermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessSystemApiPermissionService.class);

    private static final String ENABLED = "ENABLED";

    private final BizSystemMapper bizSystemMapper;
    private final BizSystemApiPermissionMapper permissionMapper;

    public BusinessSystemApiPermissionService(
            BizSystemMapper bizSystemMapper,
            BizSystemApiPermissionMapper permissionMapper) {
        this.bizSystemMapper = bizSystemMapper;
        this.permissionMapper = permissionMapper;
    }

    public void requirePermission(BusinessSystemPrincipal principal, String apiCode) {
        BizSystemPO bizSystem = requireBizSystem(principal);
        requireEnabled(bizSystem);
        requireTokenVersion(principal, bizSystem);
        requirePermissionVersion(principal, bizSystem);
        requireApiPermission(principal, apiCode);
        LOGGER.info("业务系统接口权限校验通过 业务系统ID={} 客户端ID={} 接口编码={}",
                principal.getBusinessSystemId(),
                principal.getClientId(),
                apiCode);
    }

    private BizSystemPO requireBizSystem(BusinessSystemPrincipal principal) {
        BizSystemPO bizSystem = bizSystemMapper.selectByBusinessSystemId(principal.getBusinessSystemId());
        if (bizSystem == null) {
            LOGGER.warn("业务系统不存在 业务系统ID={} 客户端ID={}",
                    principal.getBusinessSystemId(),
                    principal.getClientId());
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
        return bizSystem;
    }

    private void requireEnabled(BizSystemPO bizSystem) {
        if (!ENABLED.equals(bizSystem.getStatus())) {
            LOGGER.warn("业务系统已停用 业务系统ID={} 状态={}",
                    bizSystem.getBusinessSystemId(),
                    bizSystem.getStatus());
            throw new YundocException(YundocErrorCode.BUSINESS_SYSTEM_DISABLED);
        }
    }

    private void requireTokenVersion(BusinessSystemPrincipal principal, BizSystemPO bizSystem) {
        if (!bizSystem.getTokenVersion().equals(principal.getTokenVersion())) {
            LOGGER.warn("业务系统令牌版本不一致 业务系统ID={} 当前版本={} 最新版本={}",
                    principal.getBusinessSystemId(),
                    principal.getTokenVersion(),
                    bizSystem.getTokenVersion());
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void requirePermissionVersion(BusinessSystemPrincipal principal, BizSystemPO bizSystem) {
        if (!bizSystem.getPermissionVersion().equals(principal.getPermissionVersion())) {
            LOGGER.warn(
                    "业务系统权限版本不一致 业务系统ID={} 当前版本={} 最新版本={}",
                    principal.getBusinessSystemId(),
                    principal.getPermissionVersion(),
                    bizSystem.getPermissionVersion());
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void requireApiPermission(BusinessSystemPrincipal principal, String apiCode) {
        BizSystemApiPermissionPO permission = permissionMapper.selectByBusinessSystemIdAndApiCode(
                principal.getBusinessSystemId(),
                apiCode);
        if (permission == null) {
            LOGGER.warn("业务系统未配置接口权限 业务系统ID={} 客户端ID={} 接口编码={}",
                    principal.getBusinessSystemId(),
                    principal.getClientId(),
                    apiCode);
            throw new YundocException(YundocErrorCode.API_PERMISSION_DENIED);
        }
        requirePermissionEnabled(permission);
    }

    private void requirePermissionEnabled(BizSystemApiPermissionPO permission) {
        if (!ENABLED.equals(permission.getStatus())) {
            LOGGER.warn("业务系统接口权限已停用 业务系统ID={} 接口编码={} 状态={}",
                    permission.getBusinessSystemId(),
                    permission.getApiCode(),
                    permission.getStatus());
            throw new YundocException(YundocErrorCode.API_PERMISSION_DENIED);
        }
    }
}
