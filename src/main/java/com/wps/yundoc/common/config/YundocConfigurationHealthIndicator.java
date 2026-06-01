package com.wps.yundoc.common.config;

import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.wpsclient.infrastructure.WpsClientProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * YundocConfigurationHealthIndicator component.
 *
 * @author WPS
 */
@Component("yundocConfiguration")
public class YundocConfigurationHealthIndicator implements HealthIndicator {

    private final YundocReadinessProperties readinessProperties;
    private final WpsClientProperties wpsClientProperties;
    private final AppPreviewUploadProperties appPreviewUploadProperties;
    private final Environment environment;

    public YundocConfigurationHealthIndicator(
            YundocReadinessProperties readinessProperties,
            WpsClientProperties wpsClientProperties,
            AppPreviewUploadProperties appPreviewUploadProperties,
            Environment environment) {
        this.readinessProperties = readinessProperties;
        this.wpsClientProperties = wpsClientProperties;
        this.appPreviewUploadProperties = appPreviewUploadProperties;
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
        return Texts.isBlank(wpsClientProperties.getBaseUrl())
                || Texts.isBlank(wpsClientProperties.getPreviewPath())
                || Texts.isBlank(wpsClientProperties.getTokenPath())
                || Texts.isBlank(wpsClientProperties.getFileListPath())
                || Texts.isBlank(wpsClientProperties.getDriveListPath())
                || Texts.isBlank(wpsClientProperties.getDriveCreatePath())
                || Texts.isBlank(wpsClientProperties.getFileChildrenPathTemplate())
                || Texts.isBlank(wpsClientProperties.getFileCreatePathTemplate())
                || Texts.isBlank(wpsClientProperties.getRequestUploadPathTemplate())
                || Texts.isBlank(wpsClientProperties.getCommitUploadPathTemplate())
                || Texts.isBlank(wpsClientProperties.getAuthorizePath())
                || Texts.isBlank(wpsClientProperties.getUserTokenPath())
                || Texts.isBlank(wpsClientProperties.getRedirectUri())
                || Texts.isBlank(wpsClientProperties.getOauthScope())
                || Texts.isBlank(wpsClientProperties.getAppId())
                || Texts.isBlank(wpsClientProperties.getAppSecret())
                || Texts.isBlank(appPreviewUploadProperties.getRootParentId())
                || Texts.isBlank(appPreviewUploadProperties.getDriveName());
    }
}
