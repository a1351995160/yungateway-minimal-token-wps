package com.wps.yundoc.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityRoutePolicyTest {

    @Test
    void matchesCapabilityRouteBehindServletContextPath() {
        CapabilityRoutePolicy policy = new CapabilityRoutePolicy();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/gateway/api/v1/app/previews");
        request.setContextPath("/gateway");
        request.setServletPath("/api/v1/app/previews");

        assertThat(policy.resolve(request)).contains("app-preview:create");
    }
}
