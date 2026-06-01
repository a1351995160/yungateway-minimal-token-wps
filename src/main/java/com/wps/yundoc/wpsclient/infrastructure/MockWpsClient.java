package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateDriveRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsDriveListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileChildrenRequest;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsStoreRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;

import java.time.OffsetDateTime;
import java.util.Collections;

/**
 * MockWpsClient component.
 *
 * @author WPS
 */
public class MockWpsClient implements WpsPreviewClient, WpsAppTokenClient, WpsFileClient, WpsAuthorizationClient {

    @Override
    public WpsPreviewLink createPreview(WpsPreviewRequest request) {
        OffsetDateTime expireAt = OffsetDateTime.now().plusSeconds(request.getExpireSeconds());
        return new WpsPreviewLink("https://preview.test/files/" + request.getFileId(), expireAt);
    }

    @Override
    public WpsAppToken issueAppToken() {
        return new WpsAppToken("test-wps-app-token", OffsetDateTime.now().plusMinutes(30));
    }

    @Override
    public WpsFileList listFiles(WpsFileListRequest request) {
        return new WpsFileList(Collections.singletonList(mockFile()), "next-cursor");
    }

    @Override
    public WpsDriveList listDrives(WpsDriveListRequest request) {
        WpsDrive drive = new WpsDrive("mock-drive", "YunDoc Preview", "inuse", "yundoc");
        return new WpsDriveList(Collections.singletonList(drive), null);
    }

    @Override
    public WpsDrive createDrive(WpsCreateDriveRequest request) {
        return new WpsDrive("mock-drive-created", request.getName(), "inuse", request.getSource());
    }

    @Override
    public WpsFileList listChildren(WpsFileChildrenRequest request) {
        return new WpsFileList(Collections.singletonList(mockFolder()), null);
    }

    @Override
    public WpsFileItem createFolder(WpsCreateFolderRequest request) {
        return new WpsFileItem("mock-folder", request.getName(), "folder", true, null);
    }

    @Override
    public WpsUploadInfo requestUpload(WpsRequestUploadRequest request) {
        return new WpsUploadInfo("mock-upload", new WpsStoreRequest("PUT", "https://wps.test/upload/mock"));
    }

    @Override
    public void uploadFile(WpsUploadFileRequest request) {
        // Mock client accepts the upload.
    }

    @Override
    public WpsFileItem commitUpload(WpsCommitUploadRequest request) {
        return new WpsFileItem("mock-uploaded-file", "uploaded.docx", "file", false, null);
    }

    @Override
    public String authorizeUrl(String state) {
        return "https://wps.test/oauth/authorize?state=" + state;
    }

    @Override
    public WpsUserToken exchangeCode(String code) {
        return new WpsUserToken(
                "mock-user-access-value",
                OffsetDateTime.now().plusMinutes(30),
                "mock-user-refresh-value",
                OffsetDateTime.now().plusDays(365),
                "bearer");
    }

    @Override
    public WpsUserToken refreshToken(String refreshToken) {
        return new WpsUserToken(
                "mock-user-access-refreshed-value",
                OffsetDateTime.now().plusMinutes(30),
                "mock-user-refresh-refreshed-value",
                OffsetDateTime.now().plusDays(365),
                "bearer");
    }

    private WpsFileItem mockFile() {
        return new WpsFileItem("wps-file-001", "demo.docx", "WORD", false, "2026-05-26T18:00:00+08:00");
    }

    private WpsFileItem mockFolder() {
        return new WpsFileItem("mock-folder", "yundoc-preview-biz-app-preview-ok", "folder", true, null);
    }
}
