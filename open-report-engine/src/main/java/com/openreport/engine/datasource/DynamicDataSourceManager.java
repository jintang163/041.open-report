package com.openreport.engine.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.calcite.CalciteSchemaManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DynamicDataSourceManager {

    private final Map<String, DruidDataSource> dataSourceMap = new ConcurrentHashMap<>();

    @Autowired
    private CalciteSchemaManager calciteSchemaManager;

    public DataSource createDataSource(String dataSourceId, String dataSourceName,
                                        String driverClassName, String url,
                                        String username, String password) {
        return createDataSource(dataSourceId, dataSourceName, driverClassName, url, username, password, null);
    }

    public DataSource createDataSource(String dataSourceId, String dataSourceName,
                                        String driverClassName, String url,
                                        String username, String password,
                                        Map<String, Object> properties) {
        if (dataSourceMap.containsKey(dataSourceId)) {
            return dataSourceMap.get(dataSourceId);
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setName(dataSourceName);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                dataSource.addConnectionProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        try {
            dataSource.init();
        } catch (SQLException e) {
            log.error("Failed to initialize data source: {}", dataSourceId, e);
            throw new BusinessException("Failed to initialize data source: " + e.getMessage());
        }

        dataSourceMap.put(dataSourceId, dataSource);
        calciteSchemaManager.registerSchema(dataSourceId, dataSourceName, dataSource);
        log.info("Data source registered successfully: {}", dataSourceId);
        return dataSource;
    }

    public DataSource getDataSource(String dataSourceId) {
        DruidDataSource dataSource = dataSourceMap.get(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException("Data source not found: " + dataSourceId);
        }
        return dataSource;
    }

    public void removeDataSource(String dataSourceId) {
        DruidDataSource dataSource = dataSourceMap.remove(dataSourceId);
        if (dataSource != null) {
            dataSource.close();
            calciteSchemaManager.unregisterSchema(dataSourceId);
            log.info("Data source removed: {}", dataSourceId);
        }
    }

    public boolean exists(String dataSourceId) {
        return dataSourceMap.containsKey(dataSourceId);
    }

    public void destroy() {
        for (Map.Entry<String, DruidDataSource> entry : dataSourceMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                log.error("Failed to close data source: {}", entry.getKey(), e);
            }
        }
        dataSourceMap.clear();
    }
}
