package com.openreport.admin.service;

import com.openreport.admin.config.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DataSecurityService {

    String applyRowSecurity(String sql, Long dataSetId);

    Set<String> getHiddenFields(String tableName);

    Set<String> getMaskedFields(String tableName);

    List<Map<String, Object>> filterHiddenFields(List<Map<String, Object>> rows, String tableName);

    List<Map<String, Object>> applyFieldMasking(List<Map<String, Object>> rows, String tableName);

    List<Map<String, Object>> filterHiddenColumns(List<Map<String, Object>> columns, String tableName);
}
