package com.wps.yundoc.businesssystem.infrastructure;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * BizSystemApiPermissionPO component.
 *
 * @author WPS
 */
@Getter
@Setter
@NoArgsConstructor
public class BizSystemApiPermissionPO {

    private String businessSystemId;
    private String apiCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
