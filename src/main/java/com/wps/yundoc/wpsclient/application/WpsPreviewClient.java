package com.wps.yundoc.wpsclient.application;

/**
 * WpsPreviewClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public interface WpsPreviewClient {

    /**
     * 创建 WPS 预览链接。
     *
     * @param request 预览请求
     * @return 预览链接
     */
    WpsPreviewLink createPreview(WpsPreviewRequest request);
}
