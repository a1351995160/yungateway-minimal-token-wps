package com.wps.yundoc.auth.infrastructure;

import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * CapabilityRouteRule component.
 *
 * @author WPS
 */
class CapabilityRouteRule {

    private static final String PATH_SEPARATOR = "/";

    private final HttpMethod method;
    private final String path;
    private final String suffix;
    private final RouteMatchType matchType;
    private final String apiCode;

    CapabilityRouteRule(
            HttpMethod method,
            String path,
            String suffix,
            RouteMatchType matchType,
            String apiCode) {
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.suffix = suffix;
        this.matchType = Objects.requireNonNull(matchType, "matchType must not be null");
        this.apiCode = Objects.requireNonNull(apiCode, "apiCode must not be null");
    }

    boolean matches(HttpServletRequest request) {
        if (!method.matches(request.getMethod())) {
            return false;
        }
        return matchesPath(applicationPath(request));
    }

    String apiCode() {
        return apiCode;
    }

    private boolean matchesPath(String requestPath) {
        if (requestPath == null) {
            return false;
        }
        String normalizedPath = normalizeTrailingSlash(removePathParameters(requestPath));
        if (matchType == RouteMatchType.EXACT) {
            return path.equals(normalizedPath);
        }
        if (matchType == RouteMatchType.PREFIX) {
            return normalizedPath.startsWith(path);
        }
        return matchesSuffixRoute(normalizedPath);
    }

    private boolean matchesSuffixRoute(String requestPath) {
        if (suffix == null) {
            return false;
        }
        if (!requestPath.startsWith(path)) {
            return false;
        }
        return requestPath.endsWith(suffix);
    }

    private String applicationPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (hasText(servletPath)) {
            return servletPath;
        }
        return removeContextPath(request);
    }

    private String removeContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (!hasText(contextPath)) {
            return requestUri;
        }
        return requestUri.substring(contextPath.length());
    }

    private boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        return !value.isEmpty();
    }

    private String normalizeTrailingSlash(String value) {
        if (value == null || value.length() <= 1 || !value.endsWith(PATH_SEPARATOR)) {
            return value;
        }
        return value.substring(0, value.length() - 1);
    }

    private String removePathParameters(String value) {
        String[] segments = value.split(PATH_SEPARATOR, -1);
        StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < segments.length; index++) {
            if (index > 0) {
                normalized.append(PATH_SEPARATOR);
            }
            normalized.append(removePathParameter(segments[index]));
        }
        return normalized.toString();
    }

    private String removePathParameter(String segment) {
        int separator = segment.indexOf(';');
        if (separator < 0) {
            return segment;
        }
        return segment.substring(0, separator);
    }
}
