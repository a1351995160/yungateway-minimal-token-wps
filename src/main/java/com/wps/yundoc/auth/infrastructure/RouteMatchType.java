package com.wps.yundoc.auth.infrastructure;

/**
 * RouteMatchType 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
enum RouteMatchType {
    /**
     * 匹配完整请求路径。
     */
    EXACT,
    /**
     * 按配置前缀匹配请求路径。
     */
    PREFIX,
    /**
     * 按配置后缀匹配请求路径。
     */
    SUFFIX
}
