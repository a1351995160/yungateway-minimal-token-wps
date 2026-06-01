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

    @Test
    void matchesExactCapabilityRouteWithPathParameters() {
        CapabilityRoutePolicy policy = new CapabilityRoutePolicy();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/wps/oauth/authorize-url;foo=bar");
        request.setServletPath("/api/v1/wps/oauth/authorize-url;foo=bar");

        assertThat(policy.resolve(request)).contains("user-files:list");
    }

    @Test
    void matchesSuffixCapabilityRouteWithPathParameters() {
        CapabilityRoutePolicy policy = new CapabilityRoutePolicy();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/user/files/file-001;foo=bar/view-url;v=1");
        request.setServletPath("/api/v1/user/files/file-001;foo=bar/view-url;v=1");

        assertThat(policy.resolve(request)).contains("user-files:view");
    }
}
