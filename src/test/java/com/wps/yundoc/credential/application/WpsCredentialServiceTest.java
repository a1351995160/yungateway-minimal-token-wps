package com.wps.yundoc.credential.application;

import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.credential.infrastructure.LocalWpsTokenCache;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WpsCredentialServiceTest {

    @Test
    void fetchesAppTokenFromWpsClientAndCachesIt() {
        WpsAppTokenClient tokenClient = mock(WpsAppTokenClient.class);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(30);
        when(tokenClient.issueAppToken()).thenReturn(new WpsAppToken("app-token", expiresAt));
        WpsCredentialService service = newService(tokenClient);

        WpsCredential first = service.appCredential();
        WpsCredential second = service.appCredential();

        assertThat(first).isEqualTo(new WpsCredential("app-token", expiresAt));
        assertThat(second).isEqualTo(first);
        verify(tokenClient, times(1)).issueAppToken();
    }

    @Test
    void refreshesAppTokenWhenCachedCredentialIsInsideSkew() {
        WpsAppTokenClient tokenClient = mock(WpsAppTokenClient.class);
        OffsetDateTime newExpiresAt = OffsetDateTime.now().plusMinutes(30);
        when(tokenClient.issueAppToken()).thenReturn(new WpsAppToken("new-token", newExpiresAt));
        LocalWpsTokenCache cache = new LocalWpsTokenCache(Duration.ofSeconds(60));
        cache.put(new WpsCredential("old-token", OffsetDateTime.now().plusSeconds(30)));
        WpsCredentialService service = new WpsCredentialService(cache, tokenClient);

        WpsCredential credential = service.appCredential();

        assertThat(credential.getAccessToken()).isEqualTo("new-token");
        verify(tokenClient, times(1)).issueAppToken();
    }

    private WpsCredentialService newService(WpsAppTokenClient tokenClient) {
        LocalWpsTokenCache cache = new LocalWpsTokenCache(Duration.ofSeconds(60));
        return new WpsCredentialService(cache, tokenClient);
    }
}
