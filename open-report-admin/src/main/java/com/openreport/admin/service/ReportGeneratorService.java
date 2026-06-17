package com.openreport.admin.service;

import com.openreport.admin.dto.AiGenerateResult;

public interface ReportGeneratorService {

    GeneratedReportResult generateReport(AiGenerateResult aiResult, Long dsId, Long userId);

    GeneratedReportResult generateReportFromPrompt(String prompt, Long dsId, Long userId);

    class GeneratedReportResult {
        private Long reportId;
        private String reportName;
        private Long dataSetId;
        private String dataSetName;
        private String message;

        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }
        public Long getDataSetId() { return dataSetId; }
        public void setDataSetId(Long dataSetId) { this.dataSetId = dataSetId; }
        public String getDataSetName() { return dataSetName; }
        public void setDataSetName(String dataSetName) { this.dataSetName = dataSetName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
