package com.wps.yundoc.common.config;

import com.wps.yundoc.wpsclient.infrastructure.WpsClientProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("yundocConfiguration")
public class YundocConfigurationHealthIndicator implements HealthIndicator {

    private final YundocReadinessProperties readinessProperties;
    private final WpsClientProperties wpsClientProperties;
    private final Environment environment;

    public YundocConfigurationHealthIndicator(
            YundocReadinessProperties readinessProperties,
            WpsClientProperties wpsClientProperties,
            Environment environment) {
        this.readinessProperties = readinessProperties;
        this.wpsClientProperties = wpsClientProperties;
        this.environment = environment;
    }

    @Override
    public Health health() {
        if (requiresRealWpsClient() && missingRealWpsConfiguration()) {
            return baseHealth(Health.down())
                    .withDetail("wpsClientConfigured", false)
                    .build();
        }
        return baseHealth(Health.up())
                .withDetail("wpsClientConfigured", true)
                .build();
    }

    private Health.Builder baseHealth(Health.Builder builder) {
        return builder
                .withDetail("tdsqlRequired", readinessProperties.isTdsqlRequired())
                .withDetail("mockWpsClient", !requiresRealWpsClient());
    }

    private boolean requiresRealWpsClient() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return !activeProfiles.contains("local") && !activeProfiles.contains("test");
    }

    private boolean missingRealWpsConfiguration() {
        return missing(wpsClientProperties.getBaseUrl())
                || missing(wpsClientProperties.getPreviewPath())
                || missing(wpsClientProperties.getTokenPath())
                || missing(wpsClientProperties.getFileListPath())
                || missing(wpsClientProperties.getAuthorizePath())
                || missing(wpsClientProperties.getUserTokenPath())
                || missing(wpsClientProperties.getRedirectUri())
                || missing(wpsClientProperties.getOAuthScope())
                || missing(wpsClientProperties.getAppId())
                || missing(wpsClientProperties.getAppSecret());
    }

    private boolean missing(String value) {
        if (value == null) {
            return true;
        }
        return value.trim().isEmpty();
    }
}
