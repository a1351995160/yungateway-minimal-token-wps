package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRequestContextFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RequestContextHolder.set(RequestContext.builder(requestId(request)).build());
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (isValidRequestId(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private boolean isValidRequestId(String value) {
        if (value == null) {
            return false;
        }
        return REQUEST_ID_PATTERN.matcher(value).matches();
    }
}
