package com.wps.yundoc.wpsclient.application;

/**
 * WpsPreviewClient component.
 *
 * @author WPS
 */
public interface WpsPreviewClient {

    /**
     * Creates a WPS preview link.
     *
     * @param request preview request
     * @return preview link
     */
    WpsPreviewLink createPreview(WpsPreviewRequest request);
}
