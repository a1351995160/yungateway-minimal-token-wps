package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.capability.upload.application.FileStagingService;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.application.WpsUserAuthorizationService;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileDownloadInfo;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFileServiceTest {

    @Test
    void rejectsInsecureDownloadUrlWithoutLeakingItToCaller() {
        WpsUserAuthorizationService authorizationService = mock(WpsUserAuthorizationService.class);
        WpsFileClient fileClient = mock(WpsFileClient.class);
        UserFileService service = new UserFileService(
                authorizationService,
                fileClient,
                mock(FileStagingService.class),
                new AppPreviewUploadProperties());
        when(authorizationService.requireUserToken("user-001", "biz-001", "client-001"))
                .thenReturn(token());
        when(fileClient.downloadInfo(any()))
                .thenReturn(new WpsFileDownloadInfo("http://download.wps.test/file?sign=secret", Collections.emptyList()));

        assertThatThrownBy(() -> service.downloadInfo(new UserFileDownloadCommand(
                "user-001",
                "biz-001",
                "client-001",
                "drive-001",
                "file-001")))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR)
                .hasMessageContaining("WPS upstream error");
    }

    @Test
    void doesNotStageUploadWhenUserTokenIsMissing() {
        WpsUserAuthorizationService authorizationService = mock(WpsUserAuthorizationService.class);
        WpsFileClient fileClient = mock(WpsFileClient.class);
        FileStagingService stagingService = mock(FileStagingService.class);
        UserFileService service = new UserFileService(
                authorizationService,
                fileClient,
                stagingService,
                new AppPreviewUploadProperties());
        when(authorizationService.requireUserToken("user-001", "biz-001", "client-001"))
                .thenThrow(new YundocException(YundocErrorCode.REAUTH_REQUIRED));

        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", null, "hello".getBytes());
        assertThatThrownBy(() -> service.uploadFile(UserFileUploadCommand.builder()
                .userId("user-001")
                .businessSystemId("biz-001")
                .clientId("client-001")
                .driveId("drive-001")
                .parentFileId("folder-001")
                .file(file)
                .build()))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.REAUTH_REQUIRED);
        verify(stagingService, never()).stage(any(), any(), any(), any());
        verify(fileClient, never()).requestUpload(any());
    }

    private WpsUserToken token() {
        return new WpsUserToken(
                "user-token",
                OffsetDateTime.now().plusMinutes(30),
                "refresh-token",
                OffsetDateTime.now().plusDays(1),
                "bearer");
    }
}
