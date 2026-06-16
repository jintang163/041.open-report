package com.openreport.scheduler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.openreport"})
@MapperScan("com.openreport.scheduler.mapper")
public class OpenReportSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenReportSchedulerApplication.class, args);
    }
}
