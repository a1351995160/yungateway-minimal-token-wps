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

    @Test
    void matchesUserFileSearchDownloadAndUploadRoutes() {
        CapabilityRoutePolicy policy = new CapabilityRoutePolicy();

        MockHttpServletRequest search = new MockHttpServletRequest("GET", "/gateway/api/v1/user/files/search");
        search.setContextPath("/gateway");
        search.setServletPath("/api/v1/user/files/search");
        MockHttpServletRequest download = new MockHttpServletRequest(
                "POST",
                "/api/v1/user/files/file-001;foo=bar/download-url;v=1");
        download.setServletPath("/api/v1/user/files/file-001;foo=bar/download-url;v=1");
        MockHttpServletRequest upload = new MockHttpServletRequest("POST", "/api/v1/user/files");
        upload.setServletPath("/api/v1/user/files");

        assertThat(policy.resolve(search)).contains("user-files:search");
        assertThat(policy.resolve(download)).contains("user-files:download");
        assertThat(policy.resolve(upload)).contains("user-files:create");
    }
}
