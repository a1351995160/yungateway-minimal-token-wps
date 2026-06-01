package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.apppreview.domain.AppPreviewFolder;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewFolderMapper;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPreviewFolderServiceTest {

    private AppPreviewFolderMapper mapper;
    private WpsFileClient fileClient;

    @BeforeEach
    void setUp() {
        mapper = mock(AppPreviewFolderMapper.class);
        fileClient = mock(WpsFileClient.class);
    }

    @Test
    void recoversExistingWpsFolderWhenMappingIsMissing() {
        AppPreviewFolderService service = new AppPreviewFolderService(mapper, fileClient, properties(false));
        when(fileClient.listDrives(any()))
                .thenReturn(new WpsDriveList(Collections.singletonList(drive()), null));
        when(fileClient.listChildren(any()))
                .thenReturn(new WpsFileList(Collections.singletonList(folderForBusinessSystem("biz-001")), null));

        AppPreviewFolder result = service.ensureFolder("biz-001", "app-token");

        assertThat(result.getDriveId()).isEqualTo("drive-001");
        assertThat(result.getFolderId()).isEqualTo("folder-001");
        verify(mapper).upsert(any());
    }

    @Test
    void createsDriveOnlyWhenExplicitlyAllowed() {
        AppPreviewFolderService service = new AppPreviewFolderService(mapper, fileClient, properties(true));
        when(fileClient.listDrives(any()))
                .thenReturn(new WpsDriveList(Collections.emptyList(), null));
        when(fileClient.createDrive(any()))
                .thenReturn(drive());
        when(fileClient.listChildren(any()))
                .thenReturn(new WpsFileList(Collections.singletonList(folderForBusinessSystem("biz-001")), null));

        AppPreviewFolder result = service.ensureFolder("biz-001", "app-token");

        assertThat(result.getDriveId()).isEqualTo("drive-001");
        verify(fileClient).createDrive(any());
    }

    @Test
    void createsDistinctFoldersForCollidingSafeBusinessSystemIds() {
        AppPreviewFolderService service = new AppPreviewFolderService(mapper, fileClient, properties(false));
        when(fileClient.listDrives(any()))
                .thenReturn(new WpsDriveList(Collections.singletonList(drive()), null));
        when(fileClient.listChildren(any()))
                .thenReturn(new WpsFileList(Collections.emptyList(), null));
        when(fileClient.createFolder(any())).thenAnswer(invocation -> folderNamed(
                ((WpsCreateFolderRequest) invocation.getArgument(0)).getName()));

        AppPreviewFolder upperCase = service.ensureFolder("Foo", "app-token");
        AppPreviewFolder lowerCase = service.ensureFolder("foo", "app-token");

        assertThat(upperCase.getFolderName()).startsWith("yundoc-preview-foo-");
        assertThat(lowerCase.getFolderName()).startsWith("yundoc-preview-foo-");
        assertThat(upperCase.getFolderName()).isNotEqualTo(lowerCase.getFolderName());
    }

    @Test
    void failsWhenDriveIsMissingAndAutoCreateIsDisabled() {
        AppPreviewFolderService service = new AppPreviewFolderService(
                mock(AppPreviewFolderMapper.class),
                fileClient,
                properties(false));
        when(fileClient.listDrives(any()))
                .thenReturn(new WpsDriveList(Collections.emptyList(), null));

        assertThatThrownBy(() -> service.ensureFolder("biz-001", "app-token"))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    private AppPreviewUploadProperties properties(boolean autoCreateDrive) {
        AppPreviewUploadProperties properties = new AppPreviewUploadProperties();
        properties.setAutoCreateDrive(autoCreateDrive);
        return properties;
    }

    private WpsDrive drive() {
        return new WpsDrive("drive-001", "YunDoc Preview", "inuse", "yundoc");
    }

    private WpsFileItem folderForBusinessSystem(String businessSystemId) {
        return folderNamed(folderName(businessSystemId));
    }

    private WpsFileItem folderNamed(String folderName) {
        return new WpsFileItem("folder-001", folderName, "folder", true, null);
    }

    private String folderName(String businessSystemId) {
        return "yundoc-preview-"
                + businessSystemId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "-")
                + "-"
                + sha256Hex(businessSystemId).substring(0, 12);
    }

    private String sha256Hex(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                result.append(String.format(java.util.Locale.ROOT, "%02x", Byte.valueOf(current)));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
