package com.wps.yundoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * YundocGatewayApplication 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class YundocGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(YundocGatewayApplication.class, args);
    }
}

