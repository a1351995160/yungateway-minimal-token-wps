package com.wps.yundoc.businesssystem.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BizSystemApiPermissionMapper component.
 *
 * @author WPS
 */
@Mapper
public interface BizSystemApiPermissionMapper {

    /**
     * Selects one permission row by business system id and API code.
     *
     * @param businessSystemId business system id
     * @param apiCode API code
     * @return matching permission row, or null when absent
     */
    BizSystemApiPermissionPO selectByBusinessSystemIdAndApiCode(
            @Param("businessSystemId") String businessSystemId,
            @Param("apiCode") String apiCode);

    /**
     * Selects all permission rows for a business system.
     *
     * @param businessSystemId business system id
     * @return permission rows for the business system
     */
    List<BizSystemApiPermissionPO> selectByBusinessSystemId(
            @Param("businessSystemId") String businessSystemId);
}
