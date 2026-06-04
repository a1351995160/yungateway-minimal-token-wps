package com.wps.yundoc.wpsclient.infrastructure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WpsSignatureHeaders 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsSignatureHeaders {

    private final Map<String, String> values;

    WpsSignatureHeaders(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Map<String, String> getValues() {
        return values;
    }
}
