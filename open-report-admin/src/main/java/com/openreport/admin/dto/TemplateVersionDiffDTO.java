package com.openreport.admin.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TemplateVersionDiffDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long templateId;

    private Integer baseVersion;

    private Integer targetVersion;

    private String baseVersionName;

    private String targetVersionName;

    private String baseCreateByName;

    private String targetCreateByName;

    private LocalDateTime baseCreateTime;

    private LocalDateTime targetCreateTime;

    private List<DiffItem> diffItems;

    @Data
    public static class DiffItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String fieldName;

        private String fieldLabel;

        private String baseValue;

        private String targetValue;

        private String diffType;

        private String path;

        public DiffItem(String fieldName, String fieldLabel, String baseValue, String targetValue, String diffType) {
            this.fieldName = fieldName;
            this.fieldLabel = fieldLabel;
            this.baseValue = baseValue;
            this.targetValue = targetValue;
            this.diffType = diffType;
        }

        public DiffItem(String fieldName, String fieldLabel, String baseValue, String targetValue, String diffType, String path) {
            this.fieldName = fieldName;
            this.fieldLabel = fieldLabel;
            this.baseValue = baseValue;
            this.targetValue = targetValue;
            this.diffType = diffType;
            this.path = path;
        }
    }
}
