package com.wps.yundoc.wpsclient.application;

import java.util.List;

/**
 * WpsFileDownloadInfo 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public class WpsFileDownloadInfo {

    private final String url;
    private final List<WpsDownloadHash> hashes;

    public WpsFileDownloadInfo(String url, List<WpsDownloadHash> hashes) {
        this.url = url;
        this.hashes = hashes;
    }

    public String getUrl() {
        return url;
    }

    public List<WpsDownloadHash> getHashes() {
        return hashes;
    }
}

