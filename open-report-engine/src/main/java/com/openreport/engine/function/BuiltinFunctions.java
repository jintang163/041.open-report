package com.openreport.engine.function;

import cn.hutool.core.date.DateUtil;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.parser.CellExpressionParser;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class BuiltinFunctions {

    private static CellExpressionParser cellExpressionParser;

    public static void setCellExpressionParser(CellExpressionParser parser) {
        cellExpressionParser = parser;
    }

    public static FunctionExecutor sum() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("SUM函数需要1个参数");
            CellExpressionParser.ExpressionInfo info = cellExpressionParser.parseExpressionInfo(String.valueOf(args.get(0)));
            List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
            if (data == null) return 0;
            double sum = 0;
            for (Map<String, Object> row : data) {
                Object val = row.get(info.getFieldName());
                if (val instanceof Number) sum += ((Number) val).doubleValue();
            }
            return sum;
        };
    }

    public static FunctionExecutor avg() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("AVG函数需要1个参数");
            CellExpressionParser.ExpressionInfo info = cellExpressionParser.parseExpressionInfo(String.valueOf(args.get(0)));
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
        };
    }

    public static FunctionExecutor count() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("COUNT函数需要1个参数");
            CellExpressionParser.ExpressionInfo info = cellExpressionParser.parseExpressionInfo(String.valueOf(args.get(0)));
            List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
            if (data == null) return 0;
            int count = 0;
            for (Map<String, Object> row : data) {
                if (row.get(info.getFieldName()) != null) count++;
            }
            return count;
        };
    }

    public static FunctionExecutor max() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("MAX函数需要1个参数");
            CellExpressionParser.ExpressionInfo info = cellExpressionParser.parseExpressionInfo(String.valueOf(args.get(0)));
            List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
            if (data == null || data.isEmpty()) return null;
            double max = Double.MIN_VALUE;
            for (Map<String, Object> row : data) {
                Object val = row.get(info.getFieldName());
                if (val instanceof Number) max = Math.max(max, ((Number) val).doubleValue());
            }
            return max;
        };
    }

    public static FunctionExecutor min() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("MIN函数需要1个参数");
            CellExpressionParser.ExpressionInfo info = cellExpressionParser.parseExpressionInfo(String.valueOf(args.get(0)));
            List<Map<String, Object>> data = dataSets.get(info.getDataSetName());
            if (data == null || data.isEmpty()) return null;
            double min = Double.MAX_VALUE;
            for (Map<String, Object> row : data) {
                Object val = row.get(info.getFieldName());
                if (val instanceof Number) min = Math.min(min, ((Number) val).doubleValue());
            }
            return min;
        };
    }

    public static FunctionExecutor ifFunc() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() < 2 || args.size() > 3) throw new BusinessException("IF函数需要2或3个参数");
            Object condition = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            boolean result;
            if (condition instanceof Boolean) {
                result = (Boolean) condition;
            } else {
                result = condition != null && !"false".equalsIgnoreCase(condition.toString()) && !"0".equals(condition.toString());
            }
            if (result) {
                return cellExpressionParser.evaluateExpression(
                        String.valueOf(args.get(1)), dataSets, currentRow, parameters);
            } else if (args.size() == 3) {
                return cellExpressionParser.evaluateExpression(
                        String.valueOf(args.get(2)), dataSets, currentRow, parameters);
            }
            return null;
        };
    }

    public static FunctionExecutor concat() {
        return (args, dataSets, currentRow, parameters) -> {
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                Object val = cellExpressionParser.evaluateExpression(
                        String.valueOf(arg), dataSets, currentRow, parameters);
                if (val != null) sb.append(val);
            }
            return sb.toString();
        };
    }

    public static FunctionExecutor round() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() < 1 || args.size() > 2) throw new BusinessException("ROUND函数需要1或2个参数");
            Object valObj = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            if (!(valObj instanceof Number)) return valObj;
            double value = ((Number) valObj).doubleValue();
            int scale = 0;
            if (args.size() == 2) {
                Object scaleObj = args.get(1);
                if (scaleObj instanceof Number) {
                    scale = ((Number) scaleObj).intValue();
                }
            }
            double factor = Math.pow(10, scale);
            return Math.round(value * factor) / factor;
        };
    }

    public static FunctionExecutor dateFormat() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 2) throw new BusinessException("DATE_FORMAT函数需要2个参数");
            Object dateObj = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            String pattern = String.valueOf(args.get(1));
            if (dateObj == null) return null;
            try {
                Date date;
                if (dateObj instanceof Date) {
                    date = (Date) dateObj;
                } else if (dateObj instanceof java.sql.Date) {
                    date = new Date(((java.sql.Date) dateObj).getTime());
                } else if (dateObj instanceof java.sql.Timestamp) {
                    date = new Date(((java.sql.Timestamp) dateObj).getTime());
                } else {
                    date = DateUtil.parse(String.valueOf(dateObj));
                }
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                return sdf.format(date);
            } catch (Exception e) {
                log.warn("DATE_FORMAT failed: {}", e.getMessage());
                return dateObj.toString();
            }
        };
    }

    public static FunctionExecutor today() {
        return (args, dataSets, currentRow, parameters) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(new Date());
        };
    }

    public static FunctionExecutor now() {
        return (args, dataSets, currentRow, parameters) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date());
        };
    }

    public static FunctionExecutor upper() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("UPPER函数需要1个参数");
            Object val = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            return val == null ? null : val.toString().toUpperCase();
        };
    }

    public static FunctionExecutor lower() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("LOWER函数需要1个参数");
            Object val = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            return val == null ? null : val.toString().toLowerCase();
        };
    }

    public static FunctionExecutor substring() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() < 2 || args.size() > 3) throw new BusinessException("SUBSTRING函数需要2或3个参数");
            Object val = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            if (val == null) return null;
            String str = val.toString();
            int start = ((Number) args.get(1)).intValue() - 1;
            if (start < 0) start = 0;
            if (start >= str.length()) return "";
            if (args.size() == 3) {
                int len = ((Number) args.get(2)).intValue();
                int end = Math.min(start + len, str.length());
                return str.substring(start, end);
            }
            return str.substring(start);
        };
    }

    public static FunctionExecutor abs() {
        return (args, dataSets, currentRow, parameters) -> {
            if (args.size() != 1) throw new BusinessException("ABS函数需要1个参数");
            Object valObj = cellExpressionParser.evaluateExpression(
                    String.valueOf(args.get(0)), dataSets, currentRow, parameters);
            if (!(valObj instanceof Number)) return valObj;
            return Math.abs(((Number) valObj).doubleValue());
        };
    }
}
