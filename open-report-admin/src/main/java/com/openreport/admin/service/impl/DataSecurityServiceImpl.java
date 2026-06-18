package com.openreport.admin.service.impl;

import com.openreport.admin.config.SecurityContext;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.service.DataSecurityService;
import com.openreport.admin.service.SysFieldPermissionService;
import com.openreport.admin.service.SysRowSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataSecurityServiceImpl implements DataSecurityService {

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\bFROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WHERE_PATTERN = Pattern.compile(
            "(?i)\\bWHERE\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GROUP_BY_PATTERN = Pattern.compile(
            "(?i)\\bGROUP\\s+BY\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
            "(?i)\\bORDER\\s+BY\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HAVING_PATTERN = Pattern.compile(
            "(?i)\\bHAVING\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private SysRowSecurityService sysRowSecurityService;

    @Autowired
    private SysFieldPermissionService sysFieldPermissionService;

    @Override
    public String applyRowSecurity(String sql, Long dataSetId) {
        SecurityContext context = SecurityContextHolder.get();
        if (context == null || context.isSuperAdmin()) {
            return sql;
        }

        List<Long> roleIds = context.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return sql;
        }

        String tableName = extractMainTableName(sql);
        String filterExpression = sysRowSecurityService.buildRowFilterExpression(
                tableName, roleIds, context.getDeptId(), context.getUserId());

        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return sql;
        }

        return injectWhereClause(sql, filterExpression);
    }

    @Override
    public Set<String> getHiddenFields(String tableName) {
        SecurityContext context = SecurityContextHolder.get();
        if (context == null || context.isSuperAdmin()) {
            return Collections.emptySet();
        }
        return sysFieldPermissionService.getHiddenFields(tableName, context.getRoleIds());
    }

    @Override
    public Set<String> getMaskedFields(String tableName) {
        SecurityContext context = SecurityContextHolder.get();
        if (context == null || context.isSuperAdmin()) {
            return Collections.emptySet();
        }
        return sysFieldPermissionService.getMaskedFields(tableName, context.getRoleIds());
    }

    @Override
    public List<Map<String, Object>> filterHiddenFields(List<Map<String, Object>> rows, String tableName) {
        Set<String> hiddenFields = getHiddenFields(tableName);
        if (hiddenFields.isEmpty()) {
            return rows;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> filteredRow = new LinkedHashMap<>(row);
            for (String field : hiddenFields) {
                filteredRow.remove(field);
            }
            result.add(filteredRow);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> applyFieldMasking(List<Map<String, Object>> rows, String tableName) {
        Set<String> maskedFields = getMaskedFields(tableName);
        if (maskedFields.isEmpty()) {
            return rows;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> maskedRow = new LinkedHashMap<>(row);
            for (String field : maskedFields) {
                Object value = maskedRow.get(field);
                if (value != null) {
                    maskedRow.put(field, maskValue(value.toString()));
                }
            }
            result.add(maskedRow);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> filterHiddenColumns(List<Map<String, Object>> columns, String tableName) {
        Set<String> hiddenFields = getHiddenFields(tableName);
        if (hiddenFields.isEmpty()) {
            return columns;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> column : columns) {
            Object nameObj = column.get("name");
            Object dataIndexObj = column.get("dataIndex");
            Object keyObj = column.get("key");
            String colName = null;
            if (nameObj != null) {
                colName = nameObj.toString();
            } else if (dataIndexObj != null) {
                colName = dataIndexObj.toString();
            } else if (keyObj != null) {
                colName = keyObj.toString();
            }
            if (colName == null || !hiddenFields.contains(colName)) {
                result.add(column);
            }
        }
        return result;
    }

    private String extractMainTableName(String sql) {
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) {
            Matcher matcher = TABLE_PATTERN.matcher(sql);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "*";
    }

    private String injectWhereClause(String sql, String filterExpression) {
        String trimmedSql = sql.trim();

        if (GROUP_BY_PATTERN.matcher(trimmedSql).find()
                || HAVING_PATTERN.matcher(trimmedSql).find()) {
            return wrapWithSubquery(sql, filterExpression);
        }

        if (WHERE_PATTERN.matcher(trimmedSql).find()) {
            return trimmedSql.replaceAll("(?i)\\bWHERE\\b", " WHERE (" + filterExpression + ") AND ");
        }

        Matcher orderMatcher = ORDER_BY_PATTERN.matcher(trimmedSql);
        if (orderMatcher.find()) {
            int orderIndex = orderMatcher.start();
            String beforeOrder = trimmedSql.substring(0, orderIndex).trim();
            String orderClause = trimmedSql.substring(orderIndex);
            return beforeOrder + " WHERE (" + filterExpression + ") " + orderClause;
        }

        return trimmedSql + " WHERE (" + filterExpression + ")";
    }

    private String wrapWithSubquery(String sql, String filterExpression) {
        return "SELECT * FROM (" + sql + ") _row_sec_t WHERE (" + filterExpression + ")";
    }

    private String maskValue(String value) {
        if (value == null || value.length() <= 2) {
            return "***";
        }
        int len = value.length();
        int showLen = Math.max(1, len / 4);
        return value.substring(0, showLen) + "***" + value.substring(len - showLen);
    }
}
