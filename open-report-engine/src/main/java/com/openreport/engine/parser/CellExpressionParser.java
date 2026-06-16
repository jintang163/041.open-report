package com.openreport.engine.parser;

import com.openreport.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CellExpressionParser {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final Pattern DATASET_FIELD_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)$");

    private static final Pattern DATASET_INDEX_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\[([0-9]+)\\]\\.([a-zA-Z_][a-zA-Z0-9_]*)$");

    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\((.*)\\)$");

    public List<String> extractExpressions(String content) {
        List<String> expressions = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return expressions;
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(content);
        while (matcher.find()) {
            expressions.add(matcher.group(1));
        }
        return expressions;
    }

    public Object evaluateExpression(String expression, Map<String, List<Map<String, Object>>> dataSets,
                                      int currentRow, Map<String, Object> parameters) {
        expression = expression.trim();

        Matcher functionMatcher = FUNCTION_PATTERN.matcher(expression);
        if (functionMatcher.matches()) {
            return evaluateFunction(functionMatcher.group(1), functionMatcher.group(2), dataSets, currentRow, parameters);
        }

        Matcher indexMatcher = DATASET_INDEX_PATTERN.matcher(expression);
        if (indexMatcher.matches()) {
            String dataSetName = indexMatcher.group(1);
            int rowIndex = Integer.parseInt(indexMatcher.group(2));
            String fieldName = indexMatcher.group(3);
            return getFieldValue(dataSets, dataSetName, rowIndex, fieldName);
        }

        Matcher fieldMatcher = DATASET_FIELD_PATTERN.matcher(expression);
        if (fieldMatcher.matches()) {
            String dataSetName = fieldMatcher.group(1);
            String fieldName = fieldMatcher.group(2);
            return getFieldValue(dataSets, dataSetName, currentRow, fieldName);
        }

        if (parameters != null && parameters.containsKey(expression)) {
            return parameters.get(expression);
        }

        return expression;
    }

    public String parseContent(String content, Map<String, List<Map<String, Object>>> dataSets,
                                int currentRow, Map<String, Object> parameters) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value = evaluateExpression(expression, dataSets, currentRow, parameters);
            matcher.appendReplacement(sb, value == null ? "" : Matcher.quoteReplacement(value.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public Object parseObjectContent(String content, Map<String, List<Map<String, Object>>> dataSets,
                                      int currentRow, Map<String, Object> parameters) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(content);
        if (matcher.matches()) {
            String expression = matcher.group(1);
            return evaluateExpression(expression, dataSets, currentRow, parameters);
        }
        return parseContent(content, dataSets, currentRow, parameters);
    }

    public ExpressionInfo parseExpressionInfo(String expression) {
        expression = expression.trim();
        ExpressionInfo info = new ExpressionInfo();
        info.setOriginal(expression);

        Matcher indexMatcher = DATASET_INDEX_PATTERN.matcher(expression);
        if (indexMatcher.matches()) {
            info.setType(ExpressionType.DATASET_INDEX_FIELD);
            info.setDataSetName(indexMatcher.group(1));
            info.setRowIndex(Integer.parseInt(indexMatcher.group(2)));
            info.setFieldName(indexMatcher.group(3));
            return info;
        }

        Matcher fieldMatcher = DATASET_FIELD_PATTERN.matcher(expression);
        if (fieldMatcher.matches()) {
            info.setType(ExpressionType.DATASET_FIELD);
            info.setDataSetName(fieldMatcher.group(1));
            info.setFieldName(fieldMatcher.group(2));
            return info;
        }

        Matcher functionMatcher = FUNCTION_PATTERN.matcher(expression);
        if (functionMatcher.matches()) {
            info.setType(ExpressionType.FUNCTION);
            info.setFunctionName(functionMatcher.group(1));
            info.setFunctionArgs(functionMatcher.group(2));
            return info;
        }

        info.setType(ExpressionType.PARAMETER);
        info.setParamName(expression);
        return info;
    }

    private Object evaluateFunction(String functionName, String args, Map<String, List<Map<String, Object>>> dataSets,
                                     int currentRow, Map<String, Object> parameters) {
        List<String> argList = parseFunctionArgs(args);
        switch (functionName.toLowerCase()) {
            case "sum":
                return evaluateSum(argList, dataSets);
            case "avg":
                return evaluateAvg(argList, dataSets);
            case "count":
                return evaluateCount(argList, dataSets);
            case "max":
                return evaluateMax(argList, dataSets);
            case "min":
                return evaluateMin(argList, dataSets);
            case "if":
                return evaluateIf(argList, dataSets, currentRow, parameters);
            case "concat":
                return evaluateConcat(argList, dataSets, currentRow, parameters);
            default:
                throw new BusinessException("Unsupported function: " + functionName);
        }
    }

    private List<String> parseFunctionArgs(String args) {
        List<String> result = new ArrayList<>();
        if (args == null || args.trim().isEmpty()) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private Object evaluateSum(List<String> args, Map<String, List<Map<String, Object>>> dataSets) {
        if (args.size() != 1) throw new BusinessException("SUM function requires 1 argument");
        ExpressionInfo info = parseExpressionInfo(args.get(0));
        List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
        if (data == null) return 0;
        double sum = 0;
        for (Map<String, Object> row : data) {
            Object val = row.get(info.getFieldName());
            if (val instanceof Number) sum += ((Number) val).doubleValue();
        }
        return sum;
    }

    private Object evaluateAvg(List<String> args, Map<String, List<Map<String, Object>>> dataSets) {
        if (args.size() != 1) throw new BusinessException("AVG function requires 1 argument");
        ExpressionInfo info = parseExpressionInfo(args.get(0));
        List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
        if (data == null || data.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (Map<String, Object> row : data) {
            Object val = row.get(info.getFieldName());
            if (val instanceof Number) {
                sum += ((Number) val).doubleValue();
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private Object evaluateCount(List<String> args, Map<String, List<Map<String, Object>>> dataSets) {
        if (args.size() != 1) throw new BusinessException("COUNT function requires 1 argument");
        ExpressionInfo info = parseExpressionInfo(args.get(0));
        List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
        if (data == null) return 0;
        int count = 0;
        for (Map<String, Object> row : data) {
            if (row.get(info.getFieldName()) != null) count++;
        }
        return count;
    }

    private Object evaluateMax(List<String> args, Map<String, List<Map<String, Object>>> dataSets) {
        if (args.size() != 1) throw new BusinessException("MAX function requires 1 argument");
        ExpressionInfo info = parseExpressionInfo(args.get(0));
        List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
        if (data == null || data.isEmpty()) return null;
        double max = Double.MIN_VALUE;
        for (Map<String, Object> row : data) {
            Object val = row.get(info.getFieldName());
            if (val instanceof Number) max = Math.max(max, ((Number) val).doubleValue());
        }
        return max;
    }

    private Object evaluateMin(List<String> args, Map<String, List<Map<String, Object>>> dataSets) {
        if (args.size() != 1) throw new BusinessException("MIN function requires 1 argument");
        ExpressionInfo info = parseExpressionInfo(args.get(0));
        List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
        if (data == null || data.isEmpty()) return null;
        double min = Double.MAX_VALUE;
        for (Map<String, Object> row : data) {
            Object val = row.get(info.getFieldName());
            if (val instanceof Number) min = Math.min(min, ((Number) val).doubleValue());
        }
        return min;
    }

    private Object evaluateIf(List<String> args, Map<String, List<Map<String, Object>>> dataSets,
                               int currentRow, Map<String, Object> parameters) {
        if (args.size() < 2 || args.size() > 3) throw new BusinessException("IF function requires 2 or 3 arguments");
        Object condition = evaluateExpression(args.get(0), dataSets, currentRow, parameters);
        boolean result;
        if (condition instanceof Boolean) {
            result = (Boolean) condition;
        } else {
            result = condition != null && !"false".equalsIgnoreCase(condition.toString()) && !"0".equals(condition.toString());
        }
        if (result) {
            return evaluateExpression(args.get(1), dataSets, currentRow, parameters);
        } else if (args.size() == 3) {
            return evaluateExpression(args.get(2), dataSets, currentRow, parameters);
        }
        return null;
    }

    private Object evaluateConcat(List<String> args, Map<String, List<Map<String, Object>>> dataSets,
                                   int currentRow, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            Object val = evaluateExpression(arg, dataSets, currentRow, parameters);
            if (val != null) sb.append(val);
        }
        return sb.toString();
    }

    private Object getFieldValue(Map<String, List<Map<String, Object>>> dataSets,
                                  String dataSetName, int rowIndex, String fieldName) {
        List<Map<String, Object>> dataSet = dataSets.get(dataSetName);
        if (dataSet == null || rowIndex < 0 || rowIndex >= dataSet.size()) {
            return null;
        }
        Map<String, Object> row = dataSet.get(rowIndex);
        return row.get(fieldName);
    }

    public enum ExpressionType {
        DATASET_FIELD,
        DATASET_INDEX_FIELD,
        FUNCTION,
        PARAMETER
    }

    @lombok.Data
    public static class ExpressionInfo {
        private String original;
        private ExpressionType type;
        private String dataSetName;
        private String fieldName;
        private Integer rowIndex;
        private String functionName;
        private String functionArgs;
        private String paramName;
    }
}
