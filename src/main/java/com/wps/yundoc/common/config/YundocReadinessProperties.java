package com.wps.yundoc.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * YundocReadinessProperties 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Validated
@ConfigurationProperties(prefix = "yundoc.readiness")
public class YundocReadinessProperties {

    private boolean tdsqlRequired = true;

    public boolean isTdsqlRequired() {
        return tdsqlRequired;
    }

    public void setTdsqlRequired(boolean tdsqlRequired) {
        this.tdsqlRequired = tdsqlRequired;
    }
}
