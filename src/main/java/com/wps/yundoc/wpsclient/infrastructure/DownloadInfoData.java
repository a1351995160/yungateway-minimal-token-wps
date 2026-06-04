package com.wps.yundoc.wpsclient.infrastructure;

import java.util.List;

/**
 * DownloadInfoData 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
class DownloadInfoData {

    private String url;
    private List<DownloadHashData> hashes;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<DownloadHashData> getHashes() {
        return hashes;
    }

    public void setHashes(List<DownloadHashData> hashes) {
        this.hashes = hashes;
    }
}

