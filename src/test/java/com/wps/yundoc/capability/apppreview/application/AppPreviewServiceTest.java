package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.apppreview.domain.AppPreviewFolder;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.credential.application.WpsCredentialService;
import com.wps.yundoc.credential.domain.WpsCredential;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import com.wps.yundoc.wpsclient.application.WpsStoreRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPreviewServiceTest {

    @Test
    void uploadsFileBeforeCreatingPreview() throws Exception {
        WpsCredentialService credentialService = mock(WpsCredentialService.class);
        WpsPreviewClient previewClient = mock(WpsPreviewClient.class);
        WpsFileClient fileClient = mock(WpsFileClient.class);
        AppPreviewFileStagingService stagingService = mock(AppPreviewFileStagingService.class);
        AppPreviewFolderService folderService = mock(AppPreviewFolderService.class);
        AppPreviewUploadProperties properties = new AppPreviewUploadProperties();
        MockMultipartFile file = new MockMultipartFile("file", "demo.docx", null, "hello".getBytes());
        Path stagedPath = Files.createTempFile("app-preview-test", ".docx");
        StagedAppPreviewFile stagedFile = new StagedAppPreviewFile(
                stagedPath,
                "demo.docx",
                5L,
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        OffsetDateTime expireAt = OffsetDateTime.parse("2026-05-26T18:00:00+08:00");
        when(credentialService.appCredential()).thenReturn(credential());
        when(stagingService.stage(file, null)).thenReturn(stagedFile);
        when(folderService.ensureFolder("biz-001", "app-token"))
                .thenReturn(new AppPreviewFolder("drive-001", "folder-001", "folder"));
        when(fileClient.requestUpload(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WpsUploadInfo("upload-001", new WpsStoreRequest("PUT", "https://wps.test/upload")));
        when(fileClient.commitUpload(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WpsFileItem("wps-file-001", "demo.docx", "file", false, null));
        when(previewClient.createPreview(new WpsPreviewRequest("wps-file-001", 3600, "app-token")))
                .thenReturn(new WpsPreviewLink("https://preview", expireAt));
        AppPreviewWpsUploadService uploadService = new AppPreviewWpsUploadService(
                previewClient,
                fileClient,
                properties);
        AppPreviewService service = new AppPreviewService(
                credentialService,
                stagingService,
                folderService,
                uploadService);

        AppPreviewResult result = service.createPreview(new AppPreviewCommand("biz-001", file, null, 3600));

        assertThat(result.getPreviewUrl()).isEqualTo("https://preview");
        assertThat(result.getExpireAt()).isEqualTo(expireAt);
        assertThat(result.getFileId()).isEqualTo("wps-file-001");
        assertThat(Files.exists(stagedPath)).isFalse();
        verify(fileClient).uploadFile(org.mockito.ArgumentMatchers.any());
    }

    private WpsCredential credential() {
        return new WpsCredential("app-token", OffsetDateTime.parse("2026-05-26T17:00:00+08:00"));
    }

}
