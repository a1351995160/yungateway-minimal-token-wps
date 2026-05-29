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

    @Bean
    @Profile("!local & !test")
    public WpsHttpClient wpsHttpClient(
            WpsClientProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        return new WpsHttpClient(properties, restTemplateBuilder);
    }

    @Bean
    @Profile("!local & !test")
    public WpsPreviewClient wpsPreviewClient(WpsHttpClient wpsHttpClient) {
        return wpsHttpClient;
    }

    @Bean
    @Profile("!local & !test")
    public WpsAppTokenClient wpsAppTokenClient(WpsHttpClient wpsHttpClient) {
        return wpsHttpClient;
    }

    @Bean
    @Profile("!local & !test")
    public WpsFileClient wpsFileClient(
            WpsClientProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        return new WpsFileHttpClient(properties, restTemplateBuilder);
    }

    @Bean
    @Profile("!local & !test")
    public WpsAuthorizationClient wpsAuthorizationClient(
            WpsClientProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        return new WpsAuthorizationHttpClient(properties, restTemplateBuilder);
    }
}
