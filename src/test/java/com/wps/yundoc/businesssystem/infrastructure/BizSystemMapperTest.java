package com.wps.yundoc.businesssystem.infrastructure;

import com.wps.yundoc.businesssystem.domain.ApiCode;
import com.wps.yundoc.testsupport.BusinessSystemFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BizSystemMapperTest {

    @Autowired
    private BizSystemMapper bizSystemMapper;

    @Autowired
    private BizSystemApiPermissionMapper permissionMapper;

    @Autowired
    private BusinessSystemFixture businessSystemFixture;

    @Test
    void selectsBusinessSystemByClientId() {
        businessSystemFixture.enabled("biz-contract");

        BizSystemPO selected = bizSystemMapper.selectByClientId("cli-biz-contract");

        assertThat(selected).isNotNull();
        assertThat(selected.getBusinessSystemId()).isEqualTo("biz-contract");
        assertThat(selected.getClientSecretDigest()).hasSize(64);
        assertThat(selected.getTokenVersion()).isEqualTo(1);
        assertThat(selected.getPermissionVersion()).isEqualTo(1);
    }

    @Test
    void selectsPermissionByBusinessSystemIdAndApiCode() {
        businessSystemFixture.enabled(
                "biz-file",
                ApiCode.USER_FILES_LIST.getCode(),
                ApiCode.APP_PREVIEW_CREATE.getCode());

        BizSystemApiPermissionPO selected = permissionMapper.selectByBusinessSystemIdAndApiCode(
                "biz-file",
                ApiCode.USER_FILES_LIST.getCode());
        List<BizSystemApiPermissionPO> allPermissions = permissionMapper.selectByBusinessSystemId("biz-file");

        assertThat(selected).isNotNull();
        assertThat(selected.getStatus()).isEqualTo("ENABLED");
        assertThat(allPermissions)
                .extracting(BizSystemApiPermissionPO::getApiCode)
                .containsExactly(ApiCode.APP_PREVIEW_CREATE.getCode(), ApiCode.USER_FILES_LIST.getCode());
    }
}
