package com.wps.yundoc.wpsclient.infrastructure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WpsSignatureHeaders component.
 *
 * @author WPS
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
