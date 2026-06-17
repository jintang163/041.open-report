package com.openreport.admin.service;

import com.openreport.admin.dto.AiGenerateRequest;
import com.openreport.admin.dto.AiGenerateResult;

public interface AiService {

    AiGenerateResult generateReport(AiGenerateRequest request);

    AiGenerateResult generateSqlOnly(AiGenerateRequest request);

    boolean isEnabled();
}
