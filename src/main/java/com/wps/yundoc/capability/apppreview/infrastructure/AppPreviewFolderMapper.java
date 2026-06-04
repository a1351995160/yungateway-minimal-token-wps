package com.wps.yundoc.capability.apppreview.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AppPreviewFolderMapper 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Mapper
public interface AppPreviewFolderMapper {

    /**
     * 查询文件夹映射。
     *
     * @param businessSystemId 业务系统 ID
     * @param driveId WPS 空间 ID
     * @return 匹配的文件夹映射，不存在时返回 null
     */
    AppPreviewFolderPO selectByBusinessSystemIdAndDriveId(
            @Param("businessSystemId") String businessSystemId,
            @Param("driveId") String driveId);

    /**
     * 新增或更新文件夹映射。
     *
     * @param folder 文件夹映射
     * @return 受影响行数
     */
    int upsert(AppPreviewFolderPO folder);
}
