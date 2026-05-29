package com.wps.yundoc.businesssystem.application;

import com.wps.yundoc.auth.application.AuthTokenService;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.testsupport.BusinessSystemCredentials;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusinessSystemApiPermissionServiceTest {

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private BusinessSystemApiPermissionService permissionService;

    @Test
    void allowsConfiguredApiPermission() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-permission-allowed", "app-preview:create");
        BusinessSystemPrincipal principal = principal(credentials);

        assertThatCode(() -> permissionService.requirePermission(principal, "app-preview:create"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingApiPermission() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-permission-denied", "user-files:list");
        BusinessSystemPrincipal principal = principal(credentials);

        assertThatThrownBy(() -> permissionService.requirePermission(principal, "app-preview:create"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.API_PERMISSION_DENIED);
    }

    @Test
    void rejectsStalePermissionVersion() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-permission-stale", "app-preview:create");
        BusinessSystemPrincipal principal = principal(credentials);
        businessSystemFixture.replacePermissions("biz-permission-stale", "user-files:list");

        assertThatThrownBy(() -> permissionService.requirePermission(principal, "app-preview:create"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.TOKEN_INVALID);
    }

    @Test
    void rejectsStaleTokenVersion() {
        BusinessSystemCredentials credentials =
                businessSystemFixture.enabled("biz-token-stale", "app-preview:create");
        BusinessSystemPrincipal principal = principal(credentials);
        businessSystemFixture.rotateClientSecret("biz-token-stale");

        assertThatThrownBy(() -> permissionService.requirePermission(principal, "app-preview:create"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.TOKEN_INVALID);
    }

    private BusinessSystemPrincipal principal(BusinessSystemCredentials credentials) {
        return authTokenService.issueToken(
                credentials.getClientId(),
                credentials.getClientSecret()).getPrincipal();
    }
}
