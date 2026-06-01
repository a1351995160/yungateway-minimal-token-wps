package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateDriveRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsDriveListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileChildrenRequest;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsStoreRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadHash;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WpsFileClientTest {

    @Test
    void sendsBearerTokenWhenListingFiles() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"items\":[{\"fileId\":\"wps-file-001\","
                + "\"name\":\"demo.docx\",\"type\":\"WORD\",\"folder\":false,"
                + "\"updatedAt\":\"2026-05-26T18:00:00+08:00\"}],\"nextCursor\":\"next\"}}";
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(WpsRequestSigner.KSO_DATE_HEADER))
                        .isNotBlank())
                .andExpect(request -> assertThat(request.getHeaders().getFirst(WpsRequestSigner.KSO_AUTHORIZATION_HEADER))
                        .startsWith("KSO-1 wps-app:"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsFileList list = client.listFiles(request());

        assertThat(list.getItems()).hasSize(1);
        assertThat(list.getItems().get(0).getFileId()).isEqualTo("wps-file-001");
        assertThat(list.getNextCursor()).isEqualTo("next");
        server.verify();
    }

    @Test
    void mapsWpsFailureToStableErrorCode() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andRespond(withServerError());
        WpsFileListRequest listRequest = request();

        assertThatThrownBy(() -> client.listFiles(listRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void retriesTransientServerErrorWhenListingFiles() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"items\":[],\"nextCursor\":\"next\"}}";
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsFileList list = client.listFiles(request());

        assertThat(list.getItems()).isEmpty();
        assertThat(list.getNextCursor()).isEqualTo("next");
        server.verify();
    }

    @Test
    void mapsNullFileListItemToStableErrorCode() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"items\":[null]}}";
        server.expect(once(), requestTo("https://wps.test/api/user/files?parentFileId=root&limit=20"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        WpsFileListRequest listRequest = request();

        assertThatThrownBy(() -> client.listFiles(listRequest))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
    }

    @Test
    void listsAppDrivesWithOfficialQueryShape() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"items\":[{\"drive_id\":\"drive-001\","
                + "\"name\":\"YunDoc Preview\",\"status\":\"inuse\",\"source\":\"yundoc\"}],"
                + "\"next_page_token\":\"next-page\"}}";
        server.expect(once(), requestTo("https://wps.test/v7/drives?allotee_type=app&page_size=50"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer app-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsDriveList drives = client.listDrives(new WpsDriveListRequest("app-token", 50, null));

        assertThat(drives.getItems()).hasSize(1);
        assertThat(drives.getItems().get(0).getDriveId()).isEqualTo("drive-001");
        assertThat(drives.getNextPageToken()).isEqualTo("next-page");
        server.verify();
    }

    @Test
    void createsAppDriveWithAlloteeType() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String body = "{\"code\":0,\"data\":{\"drive_id\":\"drive-created\","
                + "\"name\":\"YunDoc Preview\",\"status\":\"inuse\",\"source\":\"yundoc\"}}";
        server.expect(once(), requestTo("https://wps.test/v7/drives/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"allotee_type\":\"app\",\"name\":\"YunDoc Preview\","
                        + "\"source\":\"yundoc\",\"total_quota\":1048576}"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        WpsDrive drive = client.createDrive(new WpsCreateDriveRequest(
                "app-token",
                "YunDoc Preview",
                "yundoc",
                1048576L));

        assertThat(drive.getDriveId()).isEqualTo("drive-created");
        server.verify();
    }

    @Test
    void listsChildrenAndCreatesFolder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        String listBody = "{\"code\":0,\"data\":{\"items\":[{\"id\":\"folder-001\","
                + "\"name\":\"yundoc-preview-biz\",\"type\":\"folder\"}]}}";
        String createBody = "{\"code\":0,\"data\":{\"id\":\"folder-created\","
                + "\"name\":\"yundoc-preview-biz\",\"type\":\"folder\"}}";
        server.expect(once(), requestTo("https://wps.test/v7/drives/drive-001/files/0/children"
                        + "?filter_type=folder&page_size=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(listBody, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://wps.test/v7/drives/drive-001/files/0/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"file_type\":\"folder\",\"name\":\"yundoc-preview-biz\","
                        + "\"on_name_conflict\":\"fail\"}"))
                .andRespond(withSuccess(createBody, MediaType.APPLICATION_JSON));

        WpsFileList children = client.listChildren(new WpsFileChildrenRequest(
                "app-token",
                "drive-001",
                "0",
                100,
                null));
        WpsFileItem folder = client.createFolder(new WpsCreateFolderRequest(
                "app-token",
                "drive-001",
                "0",
                "yundoc-preview-biz",
                "fail"));

        assertThat(children.getItems()).hasSize(1);
        assertThat(children.getItems().get(0).isFolder()).isTrue();
        assertThat(folder.getFileId()).isEqualTo("folder-created");
        server.verify();
    }

    @Test
    void requestsUploadsStreamsFileAndCommitsUpload() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(properties(), new RestTemplateBuilder(), restTemplate);
        Path file = Files.createTempFile("wps-upload-test", ".docx");
        Files.write(file, "preview-content".getBytes());
        try {
            String requestUploadBody = "{\"code\":0,\"data\":{\"upload_id\":\"upload-001\","
                    + "\"store_request\":{\"method\":\"PUT\",\"url\":\"https://upload.wps.test/files/upload-001\"}}}";
            String commitBody = "{\"code\":0,\"data\":{\"id\":\"wps-file-001\","
                    + "\"name\":\"contract.docx\",\"type\":\"file\"}}";
            server.expect(once(), requestTo("https://wps.test/v7/drives/drive-001/files/folder-001/request_upload"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json("{\"hashes\":[{\"type\":\"sha256\",\"sum\":\"abc123\"}],"
                            + "\"internal\":false,\"name\":\"contract.docx\","
                            + "\"on_name_conflict\":\"rename\",\"size\":15}"))
                    .andRespond(withSuccess(requestUploadBody, MediaType.APPLICATION_JSON));
            server.expect(once(), requestTo("https://upload.wps.test/files/upload-001"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(header("Authorization", "Bearer app-token"))
                    .andRespond(withSuccess());
            server.expect(once(), requestTo("https://wps.test/v7/drives/drive-001/files/folder-001/commit_upload"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json("{\"upload_id\":\"upload-001\"}"))
                    .andRespond(withSuccess(commitBody, MediaType.APPLICATION_JSON));

            WpsUploadInfo uploadInfo = client.requestUpload(new WpsRequestUploadRequest.Builder()
                    .accessToken("app-token")
                    .driveId("drive-001")
                    .parentFileId("folder-001")
                    .name("contract.docx")
                    .size(15L)
                    .hashes(Collections.singletonList(new WpsUploadHash("sha256", "abc123")))
                    .internal(false)
                    .onNameConflict("rename")
                    .build());
            client.uploadFile(new WpsUploadFileRequest(
                    "app-token",
                    uploadInfo.getStoreRequest(),
                    file,
                    15L,
                    "abc123"));
            WpsFileItem item = client.commitUpload(new WpsCommitUploadRequest(
                    "app-token",
                    "drive-001",
                    "folder-001",
                    uploadInfo.getUploadId()));

            assertThat(uploadInfo.getUploadId()).isEqualTo("upload-001");
            assertThat(item.getFileId()).isEqualTo("wps-file-001");
            server.verify();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsUploadUrlOutsideAllowedHostSuffixes() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        Path file = Files.createTempFile("wps-upload-test", ".docx");
        try {
            WpsUploadFileRequest request = new WpsUploadFileRequest(
                    "app-token",
                    new WpsStoreRequest("PUT", "https://wps.test.evil.example/upload"),
                    file,
                    0L,
                    "abc123");

            assertThatThrownBy(() -> client.uploadFile(request))
                    .isInstanceOf(YundocException.class)
                    .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
            server.verify();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsUnsupportedUploadMethod() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WpsFileHttpClient client = new WpsFileHttpClient(noRetryProperties(), new RestTemplateBuilder(), restTemplate);
        Path file = Files.createTempFile("wps-upload-test", ".docx");
        try {
            WpsUploadFileRequest request = new WpsUploadFileRequest(
                    "app-token",
                    new WpsStoreRequest("DELETE", "https://wps.test/upload"),
                    file,
                    0L,
                    "abc123");

            assertThatThrownBy(() -> client.uploadFile(request))
                    .isInstanceOf(YundocException.class)
                    .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.WPS_UPSTREAM_ERROR);
            server.verify();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private WpsFileListRequest request() {
        return new WpsFileListRequest("user-token", "root", 20, null);
    }

    private WpsClientProperties properties() {
        WpsClientProperties properties = new WpsClientProperties();
        properties.setBaseUrl("https://wps.test");
        properties.setFileListPath("/api/user/files");
        properties.setAppId("wps-app");
        properties.setAppSecret("wps-secret");
        properties.setUploadUrlAllowedHostSuffixes(Collections.singletonList("wps.test"));
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }

    private WpsClientProperties noRetryProperties() {
        WpsClientProperties properties = properties();
        properties.setMaxRetries(0);
        return properties;
    }
}
