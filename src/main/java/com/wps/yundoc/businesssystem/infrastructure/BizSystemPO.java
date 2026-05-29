package com.wps.yundoc.businesssystem.infrastructure;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BizSystemPO {

    private String businessSystemId;
    private String businessSystemName;
    private String clientId;
    private String clientSecretDigest;
    private String clientSecretSalt;
    private String clientSecretAlg;
    private String status;
    private Integer tokenVersion;
    private Integer permissionVersion;
    private Integer jwtTtlSeconds;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
