package com.wps.yundoc.common.util;

/**
 * Texts 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public final class Texts {

    private Texts() {
    }

    public static boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        return !value.trim().isEmpty();
    }

    public static boolean isBlank(String value) {
        return !hasText(value);
    }
}
