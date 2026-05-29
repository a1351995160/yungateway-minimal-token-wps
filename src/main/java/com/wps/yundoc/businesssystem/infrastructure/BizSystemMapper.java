package com.wps.yundoc.businesssystem.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * BizSystemMapper component.
 *
 * @author WPS
 */
@Mapper
public interface BizSystemMapper {

    /**
     * Selects one business system by business system id.
     *
     * @param businessSystemId business system id
     * @return matching business system row, or null when absent
     */
    BizSystemPO selectByBusinessSystemId(@Param("businessSystemId") String businessSystemId);

    /**
     * Selects one business system by client id.
     *
     * @param clientId client id
     * @return matching business system row, or null when absent
     */
    BizSystemPO selectByClientId(@Param("clientId") String clientId);
}
