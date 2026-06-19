package com.openreport.admin.service;

import java.util.Map;

public interface ReportExecuteService {

    Map<String, Object> executeReport(Long templateId, Map<String, Object> params,
                                      String snapshotMode, Long snapshotId);

    Map<String, Object> executeReportInternal(Long templateId, Map<String, Object> params);
}
