package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.mapper.DataSourceConfigMapper;
import com.openreport.admin.service.DataSourceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> implements DataSourceConfigService {

    @Override
    public Page<DataSourceConfig> pageList(Integer pageNum, Integer pageSize, String dsName, String dsType) {
        Page<DataSourceConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dsName)) {
            wrapper.like(DataSourceConfig::getDsName, dsName);
        }
        if (StringUtils.isNotBlank(dsType)) {
            wrapper.eq(DataSourceConfig::getDsType, dsType);
        }
        wrapper.orderByDesc(DataSourceConfig::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<DataSourceConfig> listAll() {
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceConfig::getStatus, 1);
        wrapper.orderByAsc(DataSourceConfig::getDsName);
        return list(wrapper);
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        Connection connection = null;
        try {
            Class.forName(config.getDriverClass());
            connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            log.error("测试数据库连接失败", e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public Map<String, Object> getConnectionInfo(Long id) {
        DataSourceConfig config = getById(id);
        Map<String, Object> result = new HashMap<>();
        if (config != null) {
            result.put("driverClass", config.getDriverClass());
            result.put("jdbcUrl", config.getJdbcUrl());
            result.put("username", config.getUsername());
            result.put("password", config.getPassword());
            result.put("schemaName", config.getSchemaName());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTables(Long dsId) {
        List<Map<String, Object>> tables = new ArrayList<>();
        DataSourceConfig config = getById(dsId);
        if (config == null) return tables;

        Connection connection = null;
        try {
            Class.forName(config.getDriverClass());
            connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
            DatabaseMetaData metaData = connection.getMetaData();

            String schema = config.getSchemaName();
            ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE", "VIEW"});

            while (rs.next()) {
                Map<String, Object> table = new HashMap<>();
                table.put("tableName", rs.getString("TABLE_NAME"));
                table.put("tableType", rs.getString("TABLE_TYPE"));
                table.put("remarks", rs.getString("REMARKS"));
                tables.add(table);
            }
            rs.close();
        } catch (Exception e) {
            log.error("获取数据表列表失败", e);
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) {}
            }
        }
        return tables;
    }

    @Override
    public List<Map<String, Object>> getTableColumns(Long dsId, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        DataSourceConfig config = getById(dsId);
        if (config == null) return columns;

        Connection connection = null;
        try {
            Class.forName(config.getDriverClass());
            connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
            DatabaseMetaData metaData = connection.getMetaData();

            String schema = config.getSchemaName();
            ResultSet rs = metaData.getColumns(null, schema, tableName, "%");

            while (rs.next()) {
                Map<String, Object> col = new HashMap<>();
                col.put("columnName", rs.getString("COLUMN_NAME"));
                col.put("dataType", rs.getString("TYPE_NAME"));
                col.put("columnSize", rs.getInt("COLUMN_SIZE"));
                col.put("nullable", rs.getInt("NULLABLE") == 1);
                col.put("remarks", rs.getString("REMARKS"));
                col.put("columnDef", rs.getString("COLUMN_DEF"));
                columns.add(col);
            }
            rs.close();
        } catch (Exception e) {
            log.error("获取表字段失败", e);
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) {}
            }
        }
        return columns;
    }

    @Override
    public String generateSchemaInfo(Long dsId) {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> tables = getTables(dsId);
        for (Map<String, Object> table : tables) {
            String tableName = String.valueOf(table.get("tableName"));
            sb.append("表名: ").append(tableName);
            if (table.get("remarks") != null) {
                sb.append(" (").append(table.get("remarks")).append(")");
            }
            sb.append("\n字段:\n");
            List<Map<String, Object>> columns = getTableColumns(dsId, tableName);
            for (Map<String, Object> col : columns) {
                sb.append("  - ").append(col.get("columnName"))
                        .append(" (").append(col.get("dataType"));
                if (col.get("remarks") != null && !String.valueOf(col.get("remarks")).isEmpty()) {
                    sb.append(", ").append(col.get("remarks"));
                }
                sb.append(")\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> validateSql(Long dsId, String sql) {
        Map<String, Object> result = new HashMap<>();
        DataSourceConfig config = getById(dsId);
        if (config == null) {
            result.put("success", false);
            result.put("message", "数据源不存在");
            return result;
        }
        if (StringUtils.isBlank(sql)) {
            result.put("success", false);
            result.put("message", "SQL不能为空");
            return result;
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName(config.getDriverClass());
            connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());

            String testSql = sql;
            if (!testSql.trim().toUpperCase().startsWith("EXPLAIN")) {
                if (testSql.trim().toLowerCase().startsWith("select")) {
                    testSql = "SELECT * FROM (" + sql + ") AS t LIMIT 1";
                }
            }

            ps = connection.prepareStatement(testSql);
            long start = System.currentTimeMillis();
            if (testSql.trim().toLowerCase().startsWith("select")) {
                rs = ps.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                List<Map<String, Object>> sampleData = new ArrayList<>();
                List<Map<String, String>> columns = new ArrayList<>();
                int colCount = metaData.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    Map<String, String> col = new HashMap<>();
                    col.put("name", metaData.getColumnLabel(i));
                    col.put("type", metaData.getColumnTypeName(i));
                    columns.add(col);
                }
                int rowCount = 0;
                while (rs.next() && rowCount < 5) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    sampleData.add(row);
                    rowCount++;
                }
                result.put("columns", columns);
                result.put("sampleData", sampleData);
            } else {
                ps.execute();
            }
            long duration = System.currentTimeMillis() - start;

            result.put("success", true);
            result.put("message", "SQL校验通过");
            result.put("duration", duration);
        } catch (Exception e) {
            log.error("SQL校验失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "SQL执行失败: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            if (connection != null) try { connection.close(); } catch (Exception ignored) {}
        }
        return result;
    }
}
