package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * WpsClientConfiguration component.
 *
 * @author WPS
 */
@Configuration
public class WpsClientConfiguration {

    private static final String REAL_WPS_PROFILE = "!local & !test";

    @Bean
    @Profile(REAL_WPS_PROFILE)
    public WpsHttpClient wpsHttpClient(
            WpsClientProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        return new WpsHttpClient(properties, restTemplateBuilder);
    }

    @Bean
    @Profile(REAL_WPS_PROFILE)
    public WpsPreviewClient wpsPreviewClient(WpsHttpClient wpsHttpClient) {
        return wpsHttpClient;
    }

    @Bean
    @Profile(REAL_WPS_PROFILE)
    public WpsAppTokenClient wpsAppTokenClient(WpsHttpClient wpsHttpClient) {
        return wpsHttpClient;
    }

    @Bean
    @Profile(REAL_WPS_PROFILE)
    public WpsFileClient wpsFileClient(
            WpsClientProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        return new WpsFileHttpClient(properties, restTemplateBuilder);
    }

    @Bean
    @Profile(REAL_WPS_PROFILE)
    public WpsAuthorizationClient wpsAuthorizationClient(
            WpsClientProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        return new WpsAuthorizationHttpClient(properties, restTemplateBuilder);
    }
}
