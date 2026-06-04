package com.wps.yundoc.businesssystem.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * BizSystemMapper 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@Mapper
public interface BizSystemMapper {

    /**
     * 根据业务系统 ID 查询单个业务系统。
     *
     * @param businessSystemId 业务系统 ID
     * @return 匹配的业务系统记录，不存在时返回 null
     */
    BizSystemPO selectByBusinessSystemId(@Param("businessSystemId") String businessSystemId);

    /**
     * 根据 clientId 查询单个业务系统。
     *
     * @param clientId 客户端 ID
     * @return 匹配的业务系统记录，不存在时返回 null
     */
    BizSystemPO selectByClientId(@Param("clientId") String clientId);
}
