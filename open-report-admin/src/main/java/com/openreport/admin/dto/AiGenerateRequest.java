package com.openreport.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGenerateRequest {

    private String prompt;

    private Long dsId;

    private String tableNames;

    private String schemaInfo;

    private String reportType;
}
