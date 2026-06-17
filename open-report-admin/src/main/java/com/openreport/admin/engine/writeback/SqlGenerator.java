package com.openreport.admin.engine.writeback;

import com.openreport.admin.entity.ReportWritebackConfig;
import com.openreport.admin.entity.ReportWritebackField;
import com.openreport.admin.enums.FieldTypeEnum;
import com.openreport.admin.enums.RowStatusEnum;
import com.openreport.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SqlGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String generateSql(ReportWritebackConfig config,
                              List<ReportWritebackField> fields,
                              Map<String, Object> rowData,
                              String rowStatus) {
        RowStatusEnum status = RowStatusEnum.getByCode(rowStatus);
        if (status == null) {
            throw new BusinessException("无效的行状态: " + rowStatus);
        }

        return switch (status) {
            case INSERT -> generateInsertSql(config, fields, rowData);
            case UPDATE -> generateUpdateSql(config, fields, rowData);
            case DELETE -> generateDeleteSql(config, rowData);
        };
    }

    private String generateInsertSql(ReportWritebackConfig config,
                                     List<ReportWritebackField> fields,
                                     Map<String, Object> rowData) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(config.getTableName()).append(" (");

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (ReportWritebackField field : fields) {
            String fieldName = field.getFieldName();
            if (rowData.containsKey(fieldName)) {
                columns.add(fieldName);
                placeholders.add("?");
            }
        }

        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", placeholders));
        sql.append(")");

        return sql.toString();
    }

    private String generateUpdateSql(ReportWritebackConfig config,
                                     List<ReportWritebackField> fields,
                                     Map<String, Object> rowData) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(config.getTableName()).append(" SET ");

        List<String> setClauses = new ArrayList<>();
        for (ReportWritebackField field : fields) {
            String fieldName = field.getFieldName();
            if (!fieldName.equals(config.getPrimaryKeyField())
                    && rowData.containsKey(fieldName)
                    && (field.getEditable() == null || field.getEditable() == 1)) {
                setClauses.add(fieldName + " = ?");
            }
        }

        if (StringUtils.hasText(config.getVersionField())) {
            setClauses.add(config.getVersionField() + " = " + config.getVersionField() + " + 1");
        }

        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE ").append(config.getPrimaryKeyField()).append(" = ?");

        if (StringUtils.hasText(config.getVersionField())) {
            sql.append(" AND ").append(config.getVersionField()).append(" = ?");
        }

        if (StringUtils.hasText(config.getLogicDeleteField())) {
            sql.append(" AND ").append(config.getLogicDeleteField())
               .append(" = ").append(formatValue(config.getLogicNotDeleteValue(), FieldTypeEnum.NUMBER));
        }

        return sql.toString();
    }

    private String generateDeleteSql(ReportWritebackConfig config,
                                     Map<String, Object> rowData) {
        StringBuilder sql = new StringBuilder();

        if (StringUtils.hasText(config.getLogicDeleteField())) {
            sql.append("UPDATE ").append(config.getTableName())
               .append(" SET ").append(config.getLogicDeleteField())
               .append(" = ").append(formatValue(config.getLogicDeleteValue(), FieldTypeEnum.NUMBER))
               .append(" WHERE ").append(config.getPrimaryKeyField()).append(" = ?");
        } else {
            sql.append("DELETE FROM ").append(config.getTableName())
               .append(" WHERE ").append(config.getPrimaryKeyField()).append(" = ?");
        }

        return sql.toString();
    }

    public List<Object> getParameters(ReportWritebackConfig config,
                                      List<ReportWritebackField> fields,
                                      Map<String, Object> rowData,
                                      String rowStatus) {
        RowStatusEnum status = RowStatusEnum.getByCode(rowStatus);
        List<Object> params = new ArrayList<>();

        if (status == RowStatusEnum.INSERT) {
            for (ReportWritebackField field : fields) {
                String fieldName = field.getFieldName();
                if (rowData.containsKey(fieldName)) {
                    params.add(convertValue(rowData.get(fieldName), field.getFieldType()));
                }
            }
        } else if (status == RowStatusEnum.UPDATE) {
            for (ReportWritebackField field : fields) {
                String fieldName = field.getFieldName();
                if (!fieldName.equals(config.getPrimaryKeyField())
                        && rowData.containsKey(fieldName)
                        && (field.getEditable() == null || field.getEditable() == 1)) {
                    params.add(convertValue(rowData.get(fieldName), field.getFieldType()));
                }
            }
            params.add(convertValue(rowData.get(config.getPrimaryKeyField()), FieldTypeEnum.STRING.getCode()));

            if (StringUtils.hasText(config.getVersionField()) && rowData.containsKey(config.getVersionField())) {
                params.add(convertValue(rowData.get(config.getVersionField()), FieldTypeEnum.NUMBER.getCode()));
            }
        } else if (status == RowStatusEnum.DELETE) {
            params.add(convertValue(rowData.get(config.getPrimaryKeyField()), FieldTypeEnum.STRING.getCode()));
        }

        return params;
    }

    public void setParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            int parameterIndex = i + 1;

            if (param == null) {
                ps.setObject(parameterIndex, null);
            } else if (param instanceof String) {
                ps.setString(parameterIndex, (String) param);
            } else if (param instanceof Integer) {
                ps.setInt(parameterIndex, (Integer) param);
            } else if (param instanceof Long) {
                ps.setLong(parameterIndex, (Long) param);
            } else if (param instanceof BigDecimal) {
                ps.setBigDecimal(parameterIndex, (BigDecimal) param);
            } else if (param instanceof Double) {
                ps.setDouble(parameterIndex, (Double) param);
            } else if (param instanceof Boolean) {
                ps.setBoolean(parameterIndex, (Boolean) param);
            } else if (param instanceof LocalDate) {
                ps.setDate(parameterIndex, java.sql.Date.valueOf((LocalDate) param));
            } else if (param instanceof LocalDateTime) {
                ps.setTimestamp(parameterIndex, java.sql.Timestamp.valueOf((LocalDateTime) param));
            } else {
                ps.setObject(parameterIndex, param);
            }
        }
    }

    private Object convertValue(Object value, String fieldType) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            return null;
        }

        String strValue = value.toString();
        FieldTypeEnum type = FieldTypeEnum.getByCode(fieldType);

        try {
            return switch (type) {
                case NUMBER -> new BigDecimal(strValue);
                case DATE -> LocalDate.parse(strValue, DATE_FORMATTER);
                case DATETIME -> LocalDateTime.parse(strValue, DATETIME_FORMATTER);
                case BOOLEAN -> "true".equalsIgnoreCase(strValue) || "1".equals(strValue);
                case STRING -> strValue;
            };
        } catch (Exception e) {
            log.warn("Failed to convert value {} to type {}, using original value", strValue, type);
            return strValue;
        }
    }

    private String formatValue(Object value, FieldTypeEnum type) {
        if (value == null) {
            return "NULL";
        }
        String strValue = value.toString();
        if (type == FieldTypeEnum.STRING || type == FieldTypeEnum.DATE || type == FieldTypeEnum.DATETIME) {
            return "'" + strValue.replace("'", "''") + "'";
        }
        return strValue;
    }
}
