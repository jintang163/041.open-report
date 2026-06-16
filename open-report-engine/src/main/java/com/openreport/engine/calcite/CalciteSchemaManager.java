package com.openreport.engine.calcite;

import com.openreport.common.exception.BusinessException;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CalciteSchemaManager {

    private final Map<String, CalciteJdbcSchema> schemaMap = new ConcurrentHashMap<>();

    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    public void registerSchema(String dataSourceId, String schemaName, DataSource dataSource) {
        CalciteJdbcSchema calciteJdbcSchema = new CalciteJdbcSchema(schemaName, dataSource);
        schemaMap.put(dataSourceId, calciteJdbcSchema);
        dataSourceMap.put(dataSourceId, dataSource);
    }

    public void unregisterSchema(String dataSourceId) {
        schemaMap.remove(dataSourceId);
        dataSourceMap.remove(dataSourceId);
    }

    public CalciteJdbcSchema getSchema(String dataSourceId) {
        CalciteJdbcSchema schema = schemaMap.get(dataSourceId);
        if (schema == null) {
            throw new BusinessException("Schema not registered: " + dataSourceId);
        }
        return schema;
    }

    public DataSource getDataSource(String dataSourceId) {
        DataSource dataSource = dataSourceMap.get(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException("DataSource not registered: " + dataSourceId);
        }
        return dataSource;
    }

    public boolean isRegistered(String dataSourceId) {
        return schemaMap.containsKey(dataSourceId);
    }

    public CalciteConnection createCalciteConnection() throws SQLException {
        Properties info = new Properties();
        info.setProperty("lex", "JAVA");
        Connection connection = DriverManager.getConnection("jdbc:calcite:", info);
        CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
        SchemaPlus rootSchema = calciteConnection.getRootSchema();
        for (Map.Entry<String, CalciteJdbcSchema> entry : schemaMap.entrySet()) {
            rootSchema.add(entry.getKey(), entry.getValue());
        }
        return calciteConnection;
    }
}
