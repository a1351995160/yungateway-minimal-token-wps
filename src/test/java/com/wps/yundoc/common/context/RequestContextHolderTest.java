package com.wps.yundoc.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextHolderTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void storesCurrentMvpRequestContext() {
        RequestContext context = RequestContext.builder("req-001")
                .businessSystemId("biz-contract")
                .clientId("client-contract")
                .jti("jwt-001")
                .tokenVersion(2)
                .permissionVersion(3)
                .apiCode("user-files:list")
                .userId("10001")
                .build();

        RequestContextHolder.set(context);

        assertThat(RequestContextHolder.current()).containsSame(context);
        assertThat(RequestContextHolder.currentRequestId()).contains("req-001");
        assertThat(context.getBusinessSystemId()).isEqualTo("biz-contract");
        assertThat(context.getClientId()).isEqualTo("client-contract");
        assertThat(context.getJti()).isEqualTo("jwt-001");
        assertThat(context.getTokenVersion()).isEqualTo(2);
        assertThat(context.getPermissionVersion()).isEqualTo(3);
        assertThat(context.getApiCode()).isEqualTo("user-files:list");
        assertThat(context.getUserId()).isEqualTo("10001");
    }

    @Test
    void clearRemovesCurrentContext() {
        RequestContextHolder.set(RequestContext.builder("req-002").build());

        RequestContextHolder.clear();

        assertThat(RequestContextHolder.current()).isEmpty();
        assertThat(RequestContextHolder.currentRequestId()).isEmpty();
    }
}
