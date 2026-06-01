package com.wps.yundoc.capability.apppreview.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AppPreviewFolderMapper component.
 *
 * @author WPS
 */
@Mapper
public interface AppPreviewFolderMapper {

    /**
     * Selects a folder mapping.
     *
     * @param businessSystemId business system id
     * @param driveId WPS drive id
     * @return matching folder mapping, or null
     */
    AppPreviewFolderPO selectByBusinessSystemIdAndDriveId(
            @Param("businessSystemId") String businessSystemId,
            @Param("driveId") String driveId);

    /**
     * Inserts or updates a folder mapping.
     *
     * @param folder folder mapping
     * @return affected rows
     */
    int upsert(AppPreviewFolderPO folder);
}
