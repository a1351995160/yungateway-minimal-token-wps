package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "test"})
public class MockWpsClientConfiguration {

    private final MockWpsClient mockWpsClient = new MockWpsClient();

    @Bean
    public WpsPreviewClient mockWpsPreviewClient() {
        return mockWpsClient::createPreview;
    }

    @Bean
    public WpsAppTokenClient mockWpsAppTokenClient() {
        return mockWpsClient::issueAppToken;
    }

    @Bean
    public WpsFileClient mockWpsFileClient() {
        return mockWpsClient::listFiles;
    }

    @Bean
    public WpsAuthorizationClient mockWpsAuthorizationClient() {
        return new WpsAuthorizationClient() {
            @Override
            public String authorizeUrl(String state) {
                return mockWpsClient.authorizeUrl(state);
            }

            @Override
            public WpsUserToken exchangeCode(String code) {
                return mockWpsClient.exchangeCode(code);
            }
        };
    }
}
