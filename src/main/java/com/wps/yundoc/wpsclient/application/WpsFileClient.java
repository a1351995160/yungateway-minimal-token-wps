package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileClient component.
 *
 * @author WPS
 */
public interface WpsFileClient {

    /**
     * Lists files from WPS for the request context.
     *
     * @param request file list request
     * @return file list
     */
    WpsFileList listFiles(WpsFileListRequest request);
}
