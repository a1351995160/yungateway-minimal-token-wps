package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.credential.application.WpsCredentialService;
import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPreviewServiceTest {

    @Test
    void usesAppCredentialWhenCreatingPreview() {
        WpsCredentialService credentialService = mock(WpsCredentialService.class);
        WpsPreviewClient previewClient = mock(WpsPreviewClient.class);
        OffsetDateTime expireAt = OffsetDateTime.parse("2026-05-26T18:00:00+08:00");
        when(credentialService.appCredential()).thenReturn(credential());
        when(previewClient.createPreview(any())).thenReturn(new WpsPreviewLink("https://preview", expireAt));
        AppPreviewService service = new AppPreviewService(credentialService, previewClient);

        AppPreviewResult result = service.createPreview(command());

        assertThat(result.getPreviewUrl()).isEqualTo("https://preview");
        assertThat(result.getExpireAt()).isEqualTo(expireAt);
        verify(previewClient).createPreview(new WpsPreviewRequest("wps-file-001", 3600, "app-token"));
    }

    private WpsCredential credential() {
        return new WpsCredential("app-token", OffsetDateTime.parse("2026-05-26T17:00:00+08:00"));
    }

    private AppPreviewCommand command() {
        return new AppPreviewCommand("WPS_FILE", "wps-file-001", 3600);
    }
}
