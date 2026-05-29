package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.auth.application.JwtService;
import com.wps.yundoc.auth.domain.BusinessSystemPrincipal;
import com.wps.yundoc.businesssystem.application.BusinessSystemApiPermissionService;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

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
            permissionService.requirePermission(principal, apiCode);
            RequestContextHolder.set(requestContext(principal, apiCode));
            filterChain.doFilter(request, response);
        } catch (YundocException ex) {
            errorResponseWriter.write(response, ex);
        }
    }

    private RequestContext requestContext(BusinessSystemPrincipal principal, String apiCode) {
        String requestId = RequestContextHolder.currentRequestId().orElse("unknown");
        return RequestContext.builder(requestId)
                .businessSystemId(principal.getBusinessSystemId())
                .clientId(principal.getClientId())
                .jti(principal.getJti())
                .tokenVersion(principal.getTokenVersion())
                .permissionVersion(principal.getPermissionVersion())
                .apiCode(apiCode)
                .build();
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null) {
            throw new YundocException(YundocErrorCode.AUTH_REQUIRED);
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new YundocException(YundocErrorCode.AUTH_REQUIRED);
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
