package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

/**
 * GatewayRequestContextFilter 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRequestContextFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRequestContextFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RequestContext requestContext = RequestContext.builder(requestId(request)).build();
        long startedAt = System.nanoTime();
        initializeContext(response, requestContext);
        logRequestStarted(request, requestContext);
        try {
            filterChain.doFilter(request, response);
        } finally {
            logRequestCompleted(request, response, requestContext, elapsedMillis(startedAt));
            clearContext();
        }
    }

    private void initializeContext(HttpServletResponse response, RequestContext requestContext) {
        RequestContextHolder.set(requestContext);
        MDC.put(MDC_REQUEST_ID, requestContext.getRequestId());
        response.setHeader(REQUEST_ID_HEADER, requestContext.getRequestId());
    }

    private void logRequestStarted(HttpServletRequest request, RequestContext requestContext) {
        LOGGER.info("业务系统请求开始 请求ID={} 请求方法={} 请求路径={} 请求来源IP={}",
                requestContext.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());
    }

    private void logRequestCompleted(
            HttpServletRequest request,
            HttpServletResponse response,
            RequestContext requestContext,
            long elapsedMillis) {
        LOGGER.info("业务系统请求完成 请求ID={} 请求方法={} 请求路径={} 响应状态={} 耗时毫秒={}",
                requestContext.getRequestId(),
                request.getMethod(),
                request.getRequestURI(),
                Integer.valueOf(response.getStatus()),
                Long.valueOf(elapsedMillis));
    }

    private void clearContext() {
        RequestContextHolder.clear();
        MDC.remove(MDC_REQUEST_ID);
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

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
