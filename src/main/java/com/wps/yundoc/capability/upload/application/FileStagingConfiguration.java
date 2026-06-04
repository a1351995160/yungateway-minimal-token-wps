package com.wps.yundoc.capability.upload.application;

import java.util.List;

/**
 * FileStagingConfiguration 组件。
 *
 * @author WPS
 * @date 2026-06-04 00:00:00
 */
public interface FileStagingConfiguration {

    /**
     * 获取允许暂存的最大文件大小。
     *
     * @return 最大文件字节数
     */
    long getMaxFileSizeBytes();

    /**
     * 获取允许的最大文件名长度。
     *
     * @return 最大文件名长度
     */
    int getMaxFileNameLength();

    /**
     * 获取暂存目录。
     *
     * @return 暂存目录；为空时使用系统临时目录
     */
    String getTempDirectory();

    /**
     * 获取允许的文件扩展名列表。
     *
     * @return 允许扩展名列表；为空时不限制扩展名
     */
    List<String> getAllowedExtensions();
}
