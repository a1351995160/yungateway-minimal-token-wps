package com.wps.yundoc.businesssystem.application;

import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionPO;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemPO;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.stereotype.Service;

@Service
public class BusinessSystemApiPermissionService {

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
    }

    private BizSystemPO requireBizSystem(BusinessSystemPrincipal principal) {
        BizSystemPO bizSystem = bizSystemMapper.selectByBusinessSystemId(principal.getBusinessSystemId());
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

    private void requireTokenVersion(BusinessSystemPrincipal principal, BizSystemPO bizSystem) {
        if (!bizSystem.getTokenVersion().equals(principal.getTokenVersion())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void requirePermissionVersion(BusinessSystemPrincipal principal, BizSystemPO bizSystem) {
        if (!bizSystem.getPermissionVersion().equals(principal.getPermissionVersion())) {
            throw new YundocException(YundocErrorCode.TOKEN_INVALID);
        }
    }

    private void requireApiPermission(BusinessSystemPrincipal principal, String apiCode) {
        BizSystemApiPermissionPO permission = permissionMapper.selectByBusinessSystemIdAndApiCode(
                principal.getBusinessSystemId(),
                apiCode);
        if (permission == null) {
            throw new YundocException(YundocErrorCode.API_PERMISSION_DENIED);
        }
        requirePermissionEnabled(permission);
    }

    private void requirePermissionEnabled(BizSystemApiPermissionPO permission) {
        if (!ENABLED.equals(permission.getStatus())) {
            throw new YundocException(YundocErrorCode.API_PERMISSION_DENIED);
        }
    }
}
