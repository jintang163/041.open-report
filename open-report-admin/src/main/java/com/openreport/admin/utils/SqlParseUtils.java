package com.openreport.admin.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlParseUtils {

    private static final Logger logger = LoggerFactory.getLogger(SqlParseUtils.class);

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(FROM|JOIN|UPDATE|INTO|TABLE)\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COLUMN_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\.\\s*([a-zA-Z_][a-zA-Z0-9_]*)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SELECT_COLUMN_PATTERN = Pattern.compile(
            "(?i)\\bSELECT\\s+(.*?)\\bFROM\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern ALIAS_PATTERN = Pattern.compile(
            "(?i)\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s+(?:AS\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "(?i)\\b(SUM|COUNT|AVG|MIN|MAX|DISTINCT|GROUP_CONCAT|CONCAT|SUBSTRING|TRIM|UPPER|LOWER|CAST|DATE_FORMAT|DATE_ADD|DATE_SUB)\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WHERE_CONDITION_PATTERN = Pattern.compile(
            "(?i)\\bWHERE\\s+(.*?)(?:\\bGROUP\\b|\\bORDER\\b|\\bHAVING\\b|\\bLIMIT\\b|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static class ParseResult {
        private final Set<String> tables = new LinkedHashSet<>();
        private final Set<String> columns = new LinkedHashSet<>();
        private final Map<String, String> tableAliases = new HashMap<>();
        private final Set<String> selectColumns = new LinkedHashSet<>();
        private final Set<String> whereColumns = new LinkedHashSet<>();
        private final Set<String> aggregations = new LinkedHashSet<>();
        private boolean hasAggregation = false;
        private String mainTable;

        public Set<String> getTables() { return tables; }
        public Set<String> getColumns() { return columns; }
        public Map<String, String> getTableAliases() { return tableAliases; }
        public Set<String> getSelectColumns() { return selectColumns; }
        public Set<String> getWhereColumns() { return whereColumns; }
        public Set<String> getAggregations() { return aggregations; }
        public boolean hasAggregation() { return hasAggregation; }
        public String getMainTable() { return mainTable; }
        public void setMainTable(String mainTable) { this.mainTable = mainTable; }
        public void setHasAggregation(boolean hasAggregation) { this.hasAggregation = hasAggregation; }
    }

    public static ParseResult parseSql(String sql) {
        ParseResult result = new ParseResult();
        if (StringUtils.isBlank(sql)) {
            return result;
        }

        String cleanSql = removeComments(sql).trim();

        try {
            extractTables(cleanSql, result);
            extractTableAliases(cleanSql, result);
            resolveTableAliases(result);
            extractColumns(cleanSql, result);
            extractSelectColumns(cleanSql, result);
            extractWhereColumns(cleanSql, result);
            detectAggregations(cleanSql, result);
        } catch (Exception e) {
            logger.warn("SQL解析出现非致命错误: {}", e.getMessage());
        }

        return result;
    }

    private static String removeComments(String sql) {
        sql = sql.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", " ");
        sql = sql.replaceAll("--[^\\n]*", " ");
        sql = sql.replaceAll("#[^\\n]*", " ");
        return sql;
    }

    private static void extractTables(String sql, ParseResult result) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(2);
            if (tableName != null && !tableName.equalsIgnoreCase("DUAL")) {
                result.getTables().add(tableName);
                if (result.getMainTable() == null) {
                    result.setMainTable(tableName);
                }
            }
        }
    }

    private static void extractTableAliases(String sql, ParseResult result) {
        String fromClause = extractFromClause(sql);
        if (fromClause != null) {
            Matcher matcher = ALIAS_PATTERN.matcher(fromClause);
            while (matcher.find()) {
                String table = matcher.group(1);
                String alias = matcher.group(2);
                if (result.getTables().contains(table) && !table.equalsIgnoreCase(alias)) {
                    result.getTableAliases().put(alias, table);
                }
            }
        }
    }

    private static String extractFromClause(String sql) {
        Pattern fromPattern = Pattern.compile(
                "(?i)\\bFROM\\s+(.*?)(?:\\bWHERE\\b|\\bGROUP\\b|\\bORDER\\b|\\bHAVING\\b|\\bLIMIT\\b|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = fromPattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static void resolveTableAliases(ParseResult result) {
        Set<String> resolvedTables = new LinkedHashSet<>();
        for (String table : result.getTables()) {
            resolvedTables.add(table);
        }
        result.getTables().clear();
        result.getTables().addAll(resolvedTables);
    }

    private static void extractColumns(String sql, ParseResult result) {
        Matcher matcher = COLUMN_PATTERN.matcher(sql);
        while (matcher.find()) {
            String tableOrAlias = matcher.group(1);
            String column = matcher.group(2);
            if (result.getTableAliases().containsKey(tableOrAlias)) {
                result.getColumns().add(column);
            } else if (result.getTables().contains(tableOrAlias)) {
                result.getColumns().add(column);
            }
        }
    }

    private static void extractSelectColumns(String sql, ParseResult result) {
        Matcher matcher = SELECT_COLUMN_PATTERN.matcher(sql);
        if (matcher.find()) {
            String selectPart = matcher.group(1);
            String[] columnExprs = selectPart.split(",");
            for (String expr : columnExprs) {
                expr = expr.trim();
                if (expr.equals("*")) {
                    result.getSelectColumns().add("*");
                } else {
                    String columnName = extractColumnName(expr);
                    if (columnName != null) {
                        result.getSelectColumns().add(columnName);
                    }
                }
            }
        }
    }

    private static String extractColumnName(String expr) {
        expr = expr.trim();
        int asIdx = expr.toUpperCase().lastIndexOf(" AS ");
        if (asIdx > 0) {
            String alias = expr.substring(asIdx + 4).trim();
            return alias.replaceAll("[`'\"]", "");
        }
        int spaceIdx = expr.lastIndexOf(" ");
        if (spaceIdx > 0 && spaceIdx < expr.length() - 1) {
            String lastPart = expr.substring(spaceIdx + 1).trim();
            if (!lastPart.contains("(") && !lastPart.contains(")") && !lastPart.contains(".")) {
                return lastPart.replaceAll("[`'\"]", "");
            }
        }
        int dotIdx = expr.lastIndexOf(".");
        if (dotIdx >= 0) {
            return expr.substring(dotIdx + 1).replaceAll("[`'\"]", "").trim();
        }
        String clean = expr.replaceAll("[`'\"]", "").trim();
        if (!clean.contains("(") && !clean.contains(" ")) {
            return clean;
        }
        return null;
    }

    private static void extractWhereColumns(String sql, ParseResult result) {
        Matcher matcher = WHERE_CONDITION_PATTERN.matcher(sql);
        if (matcher.find()) {
            String wherePart = matcher.group(1);
            Matcher colMatcher = COLUMN_PATTERN.matcher(wherePart);
            while (colMatcher.find()) {
                String tableOrAlias = colMatcher.group(1);
                String column = colMatcher.group(2);
                if (result.getTableAliases().containsKey(tableOrAlias) || result.getTables().contains(tableOrAlias)) {
                    result.getWhereColumns().add(column);
                }
            }
        }
    }

    private static void detectAggregations(String sql, ParseResult result) {
        Matcher matcher = FUNCTION_PATTERN.matcher(sql);
        while (matcher.find()) {
            result.getAggregations().add(matcher.group(1).toUpperCase());
            result.setHasAggregation(true);
        }
    }

    public static String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32);
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    public static String extractDatabaseNameFromJdbcUrl(String jdbcUrl) {
        if (StringUtils.isBlank(jdbcUrl)) return null;
        Pattern pattern = Pattern.compile("jdbc:[^:]+://[^/]+/([^?]+)");
        Matcher matcher = pattern.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static Set<String> extractTableNames(String sql) {
        return parseSql(sql).getTables();
    }

    public static Set<String> extractColumnNames(String sql) {
        return parseSql(sql).getColumns();
    }

    public static boolean isAggregationExpression(String expr) {
        if (StringUtils.isBlank(expr)) return false;
        Matcher matcher = FUNCTION_PATTERN.matcher(expr);
        return matcher.find();
    }
}
