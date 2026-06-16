package com.openreport.engine.calcite;

import com.openreport.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Slf4j
@Component
public class CalciteQueryExecutor {

    @Autowired
    private CalciteSchemaManager calciteSchemaManager;

    public List<Map<String, Object>> executeQuery(String dataSourceId, String sql) {
        return executeQuery(dataSourceId, sql, Collections.emptyMap());
    }

    public List<Map<String, Object>> executeQuery(String dataSourceId, String sql, Map<String, Object> parameters) {
        String processedSql = replaceParameters(sql, parameters);
        try (CalciteConnection connection = calciteSchemaManager.createCalciteConnection();
             PreparedStatement statement = connection.prepareStatement(processedSql);
             ResultSet resultSet = statement.executeQuery()) {
            return convertResultSet(resultSet);
        } catch (SQLException e) {
            log.error("Failed to execute query, dataSourceId: {}, sql: {}", dataSourceId, processedSql, e);
            throw new BusinessException("Query execution failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> executeQueryWithSchema(String schemaName, String sql, Map<String, Object> parameters) {
        String processedSql = replaceParameters(sql, parameters);
        try (CalciteConnection connection = calciteSchemaManager.createCalciteConnection()) {
            connection.setSchema(schemaName);
            try (PreparedStatement statement = connection.prepareStatement(processedSql);
                 ResultSet resultSet = statement.executeQuery()) {
                return convertResultSet(resultSet);
            }
        } catch (SQLException e) {
            log.error("Failed to execute query with schema: {}, sql: {}", schemaName, processedSql, e);
            throw new BusinessException("Query execution failed: " + e.getMessage());
        }
    }

    public Object executeSingleValue(String dataSourceId, String sql, Map<String, Object> parameters) {
        List<Map<String, Object>> results = executeQuery(dataSourceId, sql, parameters);
        if (results.isEmpty()) {
            return null;
        }
        Map<String, Object> firstRow = results.get(0);
        if (firstRow.isEmpty()) {
            return null;
        }
        return firstRow.values().iterator().next();
    }

    public Map<String, Object> executeSingleRow(String dataSourceId, String sql, Map<String, Object> parameters) {
        List<Map<String, Object>> results = executeQuery(dataSourceId, sql, parameters);
        return results.isEmpty() ? new HashMap<>() : results.get(0);
    }

    private List<Map<String, Object>> convertResultSet(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            resultList.add(row);
        }
        return resultList;
    }

    private String replaceParameters(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }
        String result = sql;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "#{" + entry.getKey() + "}";
            Object value = entry.getValue();
            String replacement = formatValue(value);
            result = result.replace(placeholder, replacement);
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            Object value = entry.getValue();
            String replacement = formatValue(value);
            result = result.replace(placeholder, replacement);
        }
        return result;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof Date || value instanceof java.sql.Date || value instanceof Timestamp) {
            return "'" + value.toString() + "'";
        }
        if (value instanceof Collection) {
            StringBuilder sb = new StringBuilder("(");
            boolean first = true;
            for (Object item : (Collection<?>) value) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(formatValue(item));
                first = false;
            }
            sb.append(")");
            return sb.toString();
        }
        return "'" + value.toString() + "'";
    }
}
