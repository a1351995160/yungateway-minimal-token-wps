package com.wps.yundoc.common.config;

import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.wpsclient.infrastructure.WpsClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class YundocConfigurationHealthIndicatorTest {

    @Test
    void healthExposesReadinessFlagsForMockProfile() {
        YundocReadinessProperties readiness = new YundocReadinessProperties();
        readiness.setTdsqlRequired(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        Health health = new YundocConfigurationHealthIndicator(
                readiness,
                new WpsClientProperties(),
                new AppPreviewUploadProperties(),
                environment).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("tdsqlRequired", true);
        assertThat(health.getDetails()).containsEntry("mockWpsClient", true);
        assertThat(health.getDetails()).containsEntry("wpsClientConfigured", true);
        assertThat(health.getDetails()).doesNotContainKey("redisRequired");
    }

    @Test
    void reportsDownWhenRealWpsConfigurationIsMissing() {
        Health health = new YundocConfigurationHealthIndicator(
                new YundocReadinessProperties(),
                new WpsClientProperties(),
                new AppPreviewUploadProperties(),
                new MockEnvironment()).health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsEntry("wpsClientConfigured", false);
    }

    @Test
    void reportsUpWhenRealWpsConfigurationIsComplete() {
        Health health = new YundocConfigurationHealthIndicator(
                new YundocReadinessProperties(),
                completeWpsProperties(),
                new AppPreviewUploadProperties(),
                new MockEnvironment()).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("mockWpsClient", false);
        assertThat(health.getDetails()).containsEntry("wpsClientConfigured", true);
    }

    @Test
    void reportsDownWhenRealAppPreviewUploadConfigurationIsMissing() {
        AppPreviewUploadProperties uploadProperties = new AppPreviewUploadProperties();
        uploadProperties.setRootParentId("");

        Health health = new YundocConfigurationHealthIndicator(
                new YundocReadinessProperties(),
                completeWpsProperties(),
                uploadProperties,
                new MockEnvironment()).health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsEntry("wpsClientConfigured", false);
    }

    private WpsClientProperties completeWpsProperties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.example.com");
        properties.setPreviewPath("/api/preview-links");
        properties.setTokenPath("/oauth2/token");
        properties.setFileListPath("/api/user/files");
        properties.setAuthorizePath("/oauth2/auth");
        properties.setUserTokenPath("/oauth2/token");
        properties.setRedirectUri("https://gateway.example.com/api/v1/wps/oauth/callback");
        properties.setOauthScope("files.read");
        properties.setAppId("wps-app-id");
        properties.setAppSecret("wps-app-secret");
        return properties;
    }
}
