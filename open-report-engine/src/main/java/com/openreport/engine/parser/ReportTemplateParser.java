package com.openreport.engine.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.model.ReportTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class ReportTemplateParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportTemplate parse(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, new TypeReference<ReportTemplate>() {});
        } catch (IOException e) {
            log.error("Failed to parse report template JSON", e);
            throw new BusinessException("Failed to parse report template: " + e.getMessage());
        }
    }

    public ReportTemplate parse(InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, new TypeReference<ReportTemplate>() {});
        } catch (IOException e) {
            log.error("Failed to parse report template from input stream", e);
            throw new BusinessException("Failed to parse report template: " + e.getMessage());
        }
    }

    public ReportTemplate parse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, new TypeReference<ReportTemplate>() {});
        } catch (IOException e) {
            log.error("Failed to parse report template from bytes", e);
            throw new BusinessException("Failed to parse report template: " + e.getMessage());
        }
    }

    public String serialize(ReportTemplate template) {
        try {
            return objectMapper.writeValueAsString(template);
        } catch (IOException e) {
            log.error("Failed to serialize report template", e);
            throw new BusinessException("Failed to serialize report template: " + e.getMessage());
        }
    }
}
