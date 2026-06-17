package com.openreport.admin.engine.writeback;

import com.openreport.admin.dto.writeback.ValidationResult;
import com.openreport.admin.entity.ReportWritebackConfig;
import com.openreport.admin.entity.ReportWritebackField;
import com.openreport.admin.enums.FieldTypeEnum;
import com.openreport.admin.enums.RowStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DataValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ValidationResult validateRow(ReportWritebackConfig config,
                                        List<ReportWritebackField> fields,
                                        Map<String, Object> rowData,
                                        String rowStatus,
                                        Integer rowIndex) {
        ValidationResult result = new ValidationResult();
        result.setRowIndex(rowIndex);

        RowStatusEnum status = RowStatusEnum.getByCode(rowStatus);
        if (status == null) {
            result.addError("第" + rowIndex + "行：无效的行状态 " + rowStatus);
            return result;
        }

        if (status == RowStatusEnum.DELETE) {
            validatePrimaryKey(config, rowData, rowIndex, result);
            return result;
        }

        for (ReportWritebackField field : fields) {
            if (field.getEditable() != null && field.getEditable() == 0) {
                continue;
            }

            String fieldName = field.getFieldName();
            Object value = rowData.get(fieldName);
            String strValue = value != null ? value.toString() : null;

            if (field.getRequired() != null && field.getRequired() == 1) {
                if (!StringUtils.hasText(strValue)) {
                    result.addError("第" + rowIndex + "行：字段[" + fieldName + "]不能为空");
                    continue;
                }
            }

            if (StringUtils.hasText(strValue)) {
                validateDataType(field, strValue, rowIndex, result);
                validateRegex(field, strValue, rowIndex, result);
            }
        }

        if (status == RowStatusEnum.UPDATE) {
            validatePrimaryKey(config, rowData, rowIndex, result);
        }

        return result;
    }

    private void validatePrimaryKey(ReportWritebackConfig config,
                                    Map<String, Object> rowData,
                                    Integer rowIndex,
                                    ValidationResult result) {
        String pkField = config.getPrimaryKeyField();
        Object pkValue = rowData.get(pkField);
        if (pkValue == null || !StringUtils.hasText(pkValue.toString())) {
            result.addError("第" + rowIndex + "行：主键字段[" + pkField + "]不能为空");
        }
    }

    private void validateDataType(ReportWritebackField field,
                                  String value,
                                  Integer rowIndex,
                                  ValidationResult result) {
        FieldTypeEnum type = FieldTypeEnum.getByCode(field.getFieldType());
        String fieldName = field.getFieldName();

        try {
            switch (type) {
                case NUMBER:
                    new BigDecimal(value);
                    break;
                case DATE:
                    LocalDate.parse(value, DATE_FORMATTER);
                    break;
                case DATETIME:
                    LocalDateTime.parse(value, DATETIME_FORMATTER);
                    break;
                case BOOLEAN:
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)
                            && !"1".equals(value) && !"0".equals(value)) {
                        result.addError("第" + rowIndex + "行：字段[" + fieldName + "]必须是布尔值(true/false或1/0)");
                    }
                    break;
                case STRING:
                default:
                    break;
            }
        } catch (Exception e) {
            result.addError("第" + rowIndex + "行：字段[" + fieldName + "]数据类型不匹配，期望类型：" + type.getDesc());
        }
    }

    private void validateRegex(ReportWritebackField field,
                               String value,
                               Integer rowIndex,
                               ValidationResult result) {
        String regex = field.getValidationRule();
        if (!StringUtils.hasText(regex)) {
            return;
        }

        try {
            if (!Pattern.matches(regex, value)) {
                String message = field.getValidationMessage();
                if (!StringUtils.hasText(message)) {
                    message = "格式不正确";
                }
                result.addError("第" + rowIndex + "行：字段[" + field.getFieldName() + "]" + message);
            }
        } catch (Exception e) {
            log.warn("Invalid validation regex for field {}: {}", field.getFieldName(), regex);
        }
    }
}
