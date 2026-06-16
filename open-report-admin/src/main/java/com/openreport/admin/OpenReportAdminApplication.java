package com.openreport.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = {"com.openreport"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.openreport\\.common\\.OpenReportCommonApplication")
})
@MapperScan("com.openreport.admin.mapper")
public class OpenReportAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenReportAdminApplication.class, args);
    }
}
