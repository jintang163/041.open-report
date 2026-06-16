package com.openreport.engine.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.openreport.common.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Slf4j
@Component
public class DataSourceConnectionTester {

    public TestResult testConnection(String driverClassName, String url,
                                      String username, String password) {
        return testConnection(driverClassName, url, username, password, null);
    }

    public TestResult testConnection(String driverClassName, String url,
                                      String username, String password,
                                      Map<String, Object> properties) {
        TestResult result = new TestResult();
        DruidDataSource dataSource = null;
        try {
            dataSource = new DruidDataSource();
            dataSource.setDriverClassName(driverClassName);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setInitialSize(1);
            dataSource.setMaxActive(1);
            dataSource.setMaxWait(10000);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestOnBorrow(true);

            if (properties != null && !properties.isEmpty()) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    dataSource.addConnectionProperty(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            long startTime = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                result.setSuccess(true);
                result.setMessage("Connection successful");
                result.setElapsedTime(elapsedTime);
                result.setCatalog(connection.getCatalog());
            }
        } catch (Exception e) {
            log.error("Connection test failed", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            result.setElapsedTime(System.currentTimeMillis());
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
        return result;
    }

    public void testConnectionOrThrow(String driverClassName, String url,
                                       String username, String password) {
        TestResult result = testConnection(driverClassName, url, username, password);
        if (!result.isSuccess()) {
            throw new BusinessException("Connection test failed: " + result.getMessage());
        }
    }

    @Data
    public static class TestResult {
        private boolean success;
        private String message;
        private long elapsedTime;
        private String catalog;
        private String schema;
    }
}
