package com.wps.yundoc.auth.infrastructure;

/**
 * RouteMatchType component.
 *
 * @author WPS
 */
enum RouteMatchType {
    /**
     * Matches the complete request path.
     */
    EXACT,
    /**
     * Matches request paths with the configured prefix.
     */
    PREFIX,
    /**
     * Matches request paths with the configured suffix.
     */
    SUFFIX
}
