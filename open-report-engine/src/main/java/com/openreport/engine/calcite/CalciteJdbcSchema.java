package com.openreport.engine.calcite;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Data
public class CalciteJdbcSchema extends AbstractSchema {

    private String schemaName;

    private DataSource dataSource;

    private Map<String, Schema> subSchemaMap = new HashMap<>();

    public CalciteJdbcSchema(String schemaName, DataSource dataSource) {
        this.schemaName = schemaName;
        this.dataSource = dataSource;
    }

    @Override
    protected Map<String, Schema> getSubSchemaMap() {
        if (subSchemaMap.isEmpty()) {
            loadSubSchemas();
        }
        return subSchemaMap;
    }

    private void loadSubSchemas() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet schemas = metaData.getSchemas();
            while (schemas.next()) {
                String schema = schemas.getString("TABLE_SCHEM");
                if (schema != null && !schema.isEmpty()) {
                    subSchemaMap.put(schema, new CalciteJdbcSubSchema(schema, dataSource));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load schemas", e);
        }
    }

    public static class CalciteJdbcSubSchema extends AbstractSchema {

        private final String schemaName;
        private final DataSource dataSource;
        private final Map<String, org.apache.calcite.schema.Table> tableMap = new HashMap<>();

        public CalciteJdbcSubSchema(String schemaName, DataSource dataSource) {
            this.schemaName = schemaName;
            this.dataSource = dataSource;
        }

        @Override
        protected Map<String, org.apache.calcite.schema.Table> getTableMap() {
            if (tableMap.isEmpty()) {
                loadTables();
            }
            return tableMap;
        }

        private void loadTables() {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet tables = metaData.getTables(null, schemaName, "%", new String[]{"TABLE", "VIEW"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName != null) {
                        tableMap.put(tableName, new CalciteJdbcTable(tableName, schemaName, dataSource));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load tables for schema: " + schemaName, e);
            }
        }
    }

    public static class CalciteJdbcTable implements org.apache.calcite.schema.Table {

        private final String tableName;
        private final String schemaName;
        private final DataSource dataSource;
        private final RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

        public CalciteJdbcTable(String tableName, String schemaName, DataSource dataSource) {
            this.tableName = tableName;
            this.schemaName = schemaName;
            this.dataSource = dataSource;
        }

        @Override
        public org.apache.calcite.rel.type.RelDataType getRowType(RelDataTypeFactory typeFactory) {
            org.apache.calcite.rel.type.RelDataTypeFactory.Builder builder = typeFactory.builder();
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet columns = metaData.getColumns(null, schemaName, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    int dataType = columns.getInt("DATA_TYPE");
                    builder.add(columnName, typeFactory.createSqlType(org.apache.calcite.sql.type.SqlTypeName.getNameForJdbcType(dataType)));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load columns for table: " + tableName, e);
            }
            return builder.build();
        }

        @Override
        public org.apache.calcite.schema.Statistic getStatistic() {
            return org.apache.calcite.schema.Statistics.UNKNOWN;
        }

        @Override
        public org.apache.calcite.schema.Schema.TableType getJdbcTableType() {
            return org.apache.calcite.schema.Schema.TableType.TABLE;
        }

        @Override
        public boolean isRolledUp(String column) {
            return false;
        }

        @Override
        public boolean rolledUpColumnValidInsideAgg(String column, org.apache.calcite.sql.SqlCall call, org.apache.calcite.sql.SqlNode parent, org.apache.calcite.rel.type.RelDataTypeFactory typeFactory) {
            return true;
        }
    }
}
