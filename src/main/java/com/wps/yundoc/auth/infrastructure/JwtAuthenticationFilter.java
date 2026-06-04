package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.auth.application.JwtService;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.businesssystem.application.BusinessSystemApiPermissionService;
import com.wps.yundoc.businesssystem.domain.ApiCode;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * JwtAuthenticationFilter 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String UNKNOWN_REQUEST_ID = "unknown";

    private final JwtService jwtService;
    private final CapabilityRoutePolicy routePolicy;
    private final AuthErrorResponseWriter errorResponseWriter;
    private final BusinessSystemApiPermissionService permissionService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CapabilityRoutePolicy routePolicy,
            AuthErrorResponseWriter errorResponseWriter,
            BusinessSystemApiPermissionService permissionService) {
        this.jwtService = jwtService;
        this.routePolicy = routePolicy;
        this.errorResponseWriter = errorResponseWriter;
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Optional<String> apiCode = routePolicy.resolve(request);
        if (!apiCode.isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }
        authenticate(request, response, filterChain, apiCode.get());
    }

    private void authenticate(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String apiCode) throws ServletException, IOException {
        try {
            BusinessSystemPrincipal principal = jwtService.validate(bearerToken(request));
            requireIdentityType(principal, apiCode);
            permissionService.requirePermission(principal, apiCode);
            RequestContextHolder.set(requestContext(principal, apiCode));
            logAuthenticated(principal, apiCode);
            filterChain.doFilter(request, response);
        } catch (YundocException ex) {
            logAuthenticationFailed(apiCode, ex);
            errorResponseWriter.write(response, ex);
        }
    }

    private RequestContext requestContext(BusinessSystemPrincipal principal, String apiCode) {
        String requestId = RequestContextHolder.currentRequestId().orElse(UNKNOWN_REQUEST_ID);
        return RequestContext.builder(requestId)
                .businessSystemId(principal.getBusinessSystemId())
                .clientId(principal.getClientId())
                .identityType(principal.getIdentityType())
                .jti(principal.getJti())
                .tokenVersion(principal.getTokenVersion())
                .permissionVersion(principal.getPermissionVersion())
                .apiCode(apiCode)
                .userId(principal.getUserId())
                .build();
    }

    private void logAuthenticated(BusinessSystemPrincipal principal, String apiCode) {
        LOGGER.info(
                "网关令牌校验通过 请求ID={} 业务系统ID={} 客户端ID={} 身份类型={} 接口编码={}",
                requestId(),
                principal.getBusinessSystemId(),
                principal.getClientId(),
                principal.getIdentityType(),
                apiCode);
    }

    private void logAuthenticationFailed(String apiCode, YundocException ex) {
        LOGGER.warn("网关令牌校验失败 请求ID={} 接口编码={} 错误码={}",
                requestId(),
                apiCode,
                ex.getErrorCode());
    }

    private String requestId() {
        return RequestContextHolder.currentRequestId().orElse(UNKNOWN_REQUEST_ID);
    }

    private void requireIdentityType(BusinessSystemPrincipal principal, String apiCode) {
        ApiCode routeApiCode = ApiCode.fromCode(apiCode)
                .orElseThrow(() -> new YundocException(YundocErrorCode.API_PERMISSION_DENIED));
        if (routeApiCode.getIdentityType() != principal.getIdentityType()) {
            throw new YundocException(YundocErrorCode.API_PERMISSION_DENIED);
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!isBearerAuthorization(authorization)) {
            throw new YundocException(YundocErrorCode.AUTH_REQUIRED);
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private boolean isBearerAuthorization(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX);
    }
}
