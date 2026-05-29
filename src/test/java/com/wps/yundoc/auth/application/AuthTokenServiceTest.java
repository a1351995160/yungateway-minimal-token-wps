package com.wps.yundoc.auth.application;

import com.wps.yundoc.businesssystem.infrastructure.BizSystemApiPermissionMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemMapper;
import com.wps.yundoc.businesssystem.infrastructure.BizSystemPO;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {

    @Test
    void rejectsDisabledBusinessSystemBeforeSecretVerification() {
        BizSystemMapper bizSystemMapper = mock(BizSystemMapper.class);
        ClientSecretDigestService digestService = mock(ClientSecretDigestService.class);
        AuthTokenService service = service(bizSystemMapper, digestService);
        when(bizSystemMapper.selectByClientId("client-disabled"))
                .thenReturn(bizSystem("DISABLED"));

        assertThatThrownBy(() -> service.issueToken("client-disabled", "secret"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.BUSINESS_SYSTEM_DISABLED);
        verifyNoInteractions(digestService);
    }

    private AuthTokenService service(
            BizSystemMapper bizSystemMapper,
            ClientSecretDigestService digestService) {
        return new AuthTokenService(
                bizSystemMapper,
                mock(BizSystemApiPermissionMapper.class),
                digestService,
                mock(JwtService.class));
    }

    private BizSystemPO bizSystem(String status) {
        BizSystemPO bizSystem = new BizSystemPO();
        bizSystem.setStatus(status);
        return bizSystem;
    }
}
