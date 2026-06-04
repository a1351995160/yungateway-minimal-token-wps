package com.wps.yundoc.businesssystem.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BizSystemApiPermissionMapper 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Mapper
public interface BizSystemApiPermissionMapper {

    /**
     * 根据业务系统 ID 和 API 编码查询单条权限记录。
     *
     * @param businessSystemId 业务系统 ID
     * @param apiCode API 编码
     * @return 匹配的权限记录，不存在时返回 null
     */
    BizSystemApiPermissionPO selectByBusinessSystemIdAndApiCode(
            @Param("businessSystemId") String businessSystemId,
            @Param("apiCode") String apiCode);

    /**
     * 查询业务系统的全部权限记录。
     *
     * @param businessSystemId 业务系统 ID
     * @return 业务系统的权限记录列表
     */
    List<BizSystemApiPermissionPO> selectByBusinessSystemId(
            @Param("businessSystemId") String businessSystemId);
}
