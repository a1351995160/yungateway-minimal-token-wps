package com.wps.yundoc.businesssystem.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BizSystemApiPermissionMapper {

    BizSystemApiPermissionPO selectByBusinessSystemIdAndApiCode(
            @Param("businessSystemId") String businessSystemId,
            @Param("apiCode") String apiCode);

    List<BizSystemApiPermissionPO> selectByBusinessSystemId(
            @Param("businessSystemId") String businessSystemId);
}
