package com.openreport.admin.dto.writeback;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WritebackConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long reportId;

    private Long dataSourceId;

    private String tableName;

    private String primaryKeyField;

    private String primaryKeyColumn;

    private String versionField;

    private String logicDeleteField;

    private String logicDeleteValue;

    private String logicNotDeleteValue;

    private Integer batchSupport;

    private Integer transactionEnable;

    private List<FieldMappingDTO> fields;

    @Data
    public static class FieldMappingDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String cellPosition;
        private String fieldName;
        private String fieldType;
        private Integer editable;
        private Integer required;
        private String defaultValue;
        private String validationRule;
        private String validationMessage;
    }
}
