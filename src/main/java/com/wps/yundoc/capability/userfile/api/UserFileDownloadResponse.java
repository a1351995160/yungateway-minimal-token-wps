package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.capability.userfile.application.UserFileDownloadResult;
import com.wps.yundoc.wpsclient.application.WpsDownloadHash;

import java.util.List;

/**
 * UserFileDownloadResponse 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileDownloadResponse {

    private final String url;
    private final List<WpsDownloadHash> hashes;

    public UserFileDownloadResponse(UserFileDownloadResult result) {
        this.url = result.getUrl();
        this.hashes = result.getHashes();
    }

    public String getUrl() {
        return url;
    }

    public List<WpsDownloadHash> getHashes() {
        return hashes;
    }
}

