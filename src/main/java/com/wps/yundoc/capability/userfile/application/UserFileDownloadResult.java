package com.wps.yundoc.capability.userfile.application;

import com.wps.yundoc.wpsclient.application.WpsDownloadHash;
import com.wps.yundoc.wpsclient.application.WpsFileDownloadInfo;

import java.util.List;

/**
 * UserFileDownloadResult 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class UserFileDownloadResult {

    private final String url;
    private final List<WpsDownloadHash> hashes;

    public UserFileDownloadResult(WpsFileDownloadInfo downloadInfo) {
        this.url = downloadInfo.getUrl();
        this.hashes = downloadInfo.getHashes();
    }

    public String getUrl() {
        return url;
    }

    public List<WpsDownloadHash> getHashes() {
        return hashes;
    }
}

