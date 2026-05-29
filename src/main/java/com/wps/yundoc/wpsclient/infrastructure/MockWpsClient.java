package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.wpsclient.application.WpsAppToken;
import com.wps.yundoc.wpsclient.application.WpsAppTokenClient;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsPreviewClient;
import com.wps.yundoc.wpsclient.application.WpsPreviewLink;
import com.wps.yundoc.wpsclient.application.WpsPreviewRequest;

import java.time.OffsetDateTime;
import java.util.Collections;

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
    public String authorizeUrl(String state) {
        return "https://wps.test/oauth/authorize?state=" + state;
    }

    @Override
    public WpsUserToken exchangeCode(String code) {
        return new WpsUserToken("mock-user-access-value", OffsetDateTime.now().plusMinutes(30));
    }

    private WpsFileItem mockFile() {
        return new WpsFileItem("wps-file-001", "demo.docx", "WORD", false, "2026-05-26T18:00:00+08:00");
    }
}
