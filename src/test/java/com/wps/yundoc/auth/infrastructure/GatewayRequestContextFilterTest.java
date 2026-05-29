package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.common.context.RequestContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRequestContextFilterTest {

    @Test
    void replacesInvalidRequestIdHeader() throws Exception {
        GatewayRequestContextFilter filter = new GatewayRequestContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicReference<String> observedRequestId = new AtomicReference<>();
        request.addHeader("X-Request-Id", "bad id with spaces");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                observedRequestId.set(RequestContextHolder.currentRequestId().orElse("")));

        assertThat(observedRequestId.get()).isNotEqualTo("bad id with spaces");
        assertThat(observedRequestId.get()).isNotBlank();
    }
}
