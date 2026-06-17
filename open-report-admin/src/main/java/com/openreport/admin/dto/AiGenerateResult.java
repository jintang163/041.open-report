package com.openreport.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGenerateResult {

    private String sql;

    private String description;

    private List<ChartSuggestion> charts;

    private List<FieldInfo> fields;

    private String reportTitle;

    @Data
    public static class ChartSuggestion {
        private String chartType;
        private String title;
        private String xField;
        private List<String> yFields;
        private String description;
    }

    @Data
    public static class FieldInfo {
        private String name;
        private String type;
        private String label;
    }
}
