package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPreviewFileStagingServiceTest {

    @Test
    void stagesFileWithSha256AndCleansTempFile() {
        AppPreviewFileStagingService service = new AppPreviewFileStagingService(properties(10L));
        MockMultipartFile file = new MockMultipartFile("file", "demo.docx", null, "hello".getBytes());

        StagedAppPreviewFile staged = service.stage(file, null);

        assertThat(staged.getFileName()).isEqualTo("demo.docx");
        assertThat(staged.getSize()).isEqualTo(5L);
        assertThat(staged.getSha256())
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(Files.exists(staged.getPath())).isTrue();

        staged.close();

        assertThat(Files.exists(staged.getPath())).isFalse();
    }

    @Test
    void rejectsFileLargerThanLimit() {
        AppPreviewFileStagingService service = new AppPreviewFileStagingService(properties(4L));
        MockMultipartFile file = new MockMultipartFile("file", "demo.docx", null, "hello".getBytes());

        assertThatThrownBy(() -> service.stage(file, null))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.VALIDATION_FAILED);
    }

    @Test
    void rejectsUnsafeFileName() {
        AppPreviewFileStagingService service = new AppPreviewFileStagingService(properties(10L));
        MockMultipartFile file = new MockMultipartFile("file", "../secret.docx", null, "hello".getBytes());

        assertThatThrownBy(() -> service.stage(file, null))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.VALIDATION_FAILED);
    }

    private AppPreviewUploadProperties properties(long maxBytes) {
        AppPreviewUploadProperties properties = new AppPreviewUploadProperties();
        properties.setMaxFileSizeBytes(maxBytes);
        return properties;
    }
}
