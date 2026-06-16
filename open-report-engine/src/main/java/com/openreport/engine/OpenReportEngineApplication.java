package com.openreport.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.openreport", exclude = {DataSourceAutoConfiguration.class})
public class OpenReportEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenReportEngineApplication.class, args);
    }
}
