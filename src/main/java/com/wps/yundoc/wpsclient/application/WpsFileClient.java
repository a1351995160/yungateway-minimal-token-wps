package com.wps.yundoc.wpsclient.application;

/**
 * WpsFileClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public interface WpsFileClient {

    /**
     * 按请求上下文查询 WPS 文件列表。
     *
     * @param request 文件列表请求
     * @return 文件列表
     */
    WpsFileList listFiles(WpsFileListRequest request);

    /**
     * 查询当前身份下的 WPS 空间列表。
     *
     * @param request 空间列表请求
     * @return 空间列表
     */
    WpsDriveList listDrives(WpsDriveListRequest request);

    /**
     * 创建 WPS 空间。
     *
     * @param request 创建空间请求
     * @return 已创建的空间
     */
    WpsDrive createDrive(WpsCreateDriveRequest request);

    /**
     * 查询 WPS 文件夹下的子文件。
     *
     * @param request 子文件列表请求
     * @return 子文件列表
     */
    WpsFileList listChildren(WpsFileChildrenRequest request);

    /**
     * 创建 WPS 文件夹。
     *
     * @param request 创建文件夹请求
     * @return 已创建的文件夹
     */
    WpsFileItem createFolder(WpsCreateFolderRequest request);

    /**
     * 请求 WPS 上传信息。
     *
     * @param request 上传请求
     * @return 上传信息
     */
    WpsUploadInfo requestUpload(WpsRequestUploadRequest request);

    /**
     * 将实体文件上传到 WPS 存储地址。
     *
     * @param request 实体上传请求
     */
    void uploadFile(WpsUploadFileRequest request);

    /**
     * 提交 WPS 上传完成。
     *
     * @param request 提交上传请求
     * @return 已提交的 WPS 文件
     */
    WpsFileItem commitUpload(WpsCommitUploadRequest request);
}
