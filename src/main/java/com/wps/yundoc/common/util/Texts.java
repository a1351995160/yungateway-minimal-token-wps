package com.wps.yundoc.common.util;

/**
 * Texts component.
 *
 * @author WPS
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
