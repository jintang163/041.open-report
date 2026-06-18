package com.openreport.engine.pivot;

import com.openreport.engine.pivot.model.PivotDataCell;
import com.openreport.engine.pivot.model.PivotField;
import com.openreport.engine.pivot.model.PivotHeaderCell;
import com.openreport.engine.pivot.model.PivotTableConfig;
import com.openreport.engine.pivot.model.PivotTableResult;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 交叉报表核心引擎
 */
public class PivotTableEngine {

    private static final String DELIMITER = "||";

    /**
     * 生成分组查询SQL
     */
    public String buildGroupBySql(String baseSql, PivotTableConfig config) {
        if (StringUtils.isBlank(baseSql) || config == null) {
            return baseSql;
        }

        StringBuilder selectClause = new StringBuilder();
        StringBuilder groupByClause = new StringBuilder();

        List<PivotField> allGroupFields = new ArrayList<>();
        if (config.getRowFields() != null) {
            allGroupFields.addAll(config.getRowFields());
        }
        if (config.getColumnFields() != null) {
            allGroupFields.addAll(config.getColumnFields());
        }

        for (int i = 0; i < allGroupFields.size(); i++) {
            PivotField field = allGroupFields.get(i);
            if (i > 0) {
                selectClause.append(", ");
                groupByClause.append(", ");
            }
            selectClause.append(field.getFieldName());
            groupByClause.append(field.getFieldName());
        }

        if (!allGroupFields.isEmpty() && config.getValueFields() != null && !config.getValueFields().isEmpty()) {
            selectClause.append(", ");
        }

        if (config.getValueFields() != null) {
            for (int i = 0; i < config.getValueFields().size(); i++) {
                PivotField valueField = config.getValueFields().get(i);
                if (i > 0) {
                    selectClause.append(", ");
                }
                String aggFunc = StringUtils.defaultIfBlank(valueField.getAggregateFunction(), "SUM");
                String alias = valueField.getFieldName() + "_" + aggFunc.toLowerCase();
                selectClause.append(aggFunc).append("(").append(valueField.getFieldName()).append(") AS ").append(alias);
            }
        }

        String tableName = extractMainTableName(baseSql);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectClause);
        sql.append(" FROM ").append(tableName);
        if (!allGroupFields.isEmpty()) {
            sql.append(" GROUP BY ").append(groupByClause);
        }

        return sql.toString();
    }

    /**
     * 数据透视转换
     */
    public PivotTableResult pivotResultSet(List<Map<String, Object>> rawData, PivotTableConfig config) {
        PivotTableResult result = new PivotTableResult();

        if (rawData == null || rawData.isEmpty()) {
            result.setRowHeaders(new ArrayList<>());
            result.setColumnHeaders(new ArrayList<>());
            result.setDataCells(new ArrayList<>());
            result.setSummary(new HashMap<>());
            result.setDrillDownFields(new ArrayList<>());
            return result;
        }

        List<PivotField> rowFields = config.getRowFields() != null ? config.getRowFields() : new ArrayList<>();
        List<PivotField> colFields = config.getColumnFields() != null ? config.getColumnFields() : new ArrayList<>();
        List<PivotField> valFields = config.getValueFields() != null ? config.getValueFields() : new ArrayList<>();

        Set<List<Object>> colValueSet = new LinkedHashSet<>();
        Set<List<Object>> rowValueSet = new LinkedHashSet<>();
        Map<String, Map<String, Object>> dataMap = new HashMap<>();

        for (Map<String, Object> row : rawData) {
            List<Object> rowValues = rowFields.stream()
                    .map(f -> row.get(f.getFieldName()))
                    .collect(Collectors.toList());
            List<Object> colValues = colFields.stream()
                    .map(f -> row.get(f.getFieldName()))
                    .collect(Collectors.toList());

            rowValueSet.add(rowValues);
            colValueSet.add(colValues);

            String rowKey = buildKey(rowValues);
            String colKey = buildKey(colValues);

            dataMap.computeIfAbsent(rowKey, k -> new HashMap<>());

            for (PivotField valField : valFields) {
                String aggFunc = StringUtils.defaultIfBlank(valField.getAggregateFunction(), "SUM");
                String valueKey = valField.getFieldName() + "_" + aggFunc.toLowerCase();
                Object value = row.get(valueKey);
                if (value == null) {
                    value = row.get(valField.getFieldName());
                }
                dataMap.get(rowKey).put(colKey + DELIMITER + valField.getFieldName(), value);
            }
        }

        List<List<Object>> rowCombos = new ArrayList<>(rowValueSet);
        List<List<Object>> colCombos = new ArrayList<>(colValueSet);

        List<List<PivotHeaderCell>> rowHeaders = buildRowHeaders(config, rowCombos);
        List<List<PivotHeaderCell>> columnHeaders = buildColumnHeaders(config, colCombos);
        List<List<PivotDataCell>> dataCells = buildDataMatrix(config, dataMap, rowCombos, colCombos);

        if (Boolean.TRUE.equals(config.getShowSubtotal()) || Boolean.TRUE.equals(config.getShowGrandTotal())) {
            calculateSubtotalsAndTotals(result, dataMap, rowCombos, colCombos, config);
        }

        result.setRowHeaders(rowHeaders);
        result.setColumnHeaders(columnHeaders);
        result.setDataCells(dataCells);

        Map<String, Object> summary = new HashMap<>();
        summary.put("rowCount", rowCombos.size());
        summary.put("columnCount", colCombos.size() * valFields.size());
        summary.put("dataCount", rawData.size());
        result.setSummary(summary);

        List<String> drillDownFields = rowFields.stream()
                .map(PivotField::getFieldName)
                .collect(Collectors.toList());
        result.setDrillDownFields(drillDownFields);

        return result;
    }

    /**
     * 构建多层列表头
     */
    public List<List<PivotHeaderCell>> buildColumnHeaders(PivotTableConfig config, List<List<Object>> columnValueCombinations) {
        List<List<PivotHeaderCell>> headers = new ArrayList<>();
        List<PivotField> colFields = config.getColumnFields() != null ? config.getColumnFields() : new ArrayList<>();
        List<PivotField> valFields = config.getValueFields() != null ? config.getValueFields() : new ArrayList<>();

        int colLevelCount = colFields.size();
        int valCount = valFields.size();

        for (int level = 0; level < colLevelCount; level++) {
            List<PivotHeaderCell> levelHeaders = new ArrayList<>();
            PivotField field = colFields.get(level);

            Map<Object, Integer> valueSpanMap = new LinkedHashMap<>();
            for (List<Object> combo : columnValueCombinations) {
                Object value = combo.get(level);
                valueSpanMap.merge(value, valCount, Integer::sum);
            }

            for (Map.Entry<Object, Integer> entry : valueSpanMap.entrySet()) {
                PivotHeaderCell cell = new PivotHeaderCell();
                cell.setValue(entry.getValue() == null ? "" : entry.getValue());
                cell.setFieldValue(entry.getKey());
                cell.setFieldName(field.getFieldName());
                cell.setLevel(level);
                cell.setColSpan(entry.getValue());
                cell.setRowSpan(1);
                cell.setIsLeaf(level == colLevelCount - 1);
                levelHeaders.add(cell);
            }

            headers.add(levelHeaders);
        }

        if (colLevelCount > 0 && valCount > 1) {
            List<PivotHeaderCell> valHeaderRow = new ArrayList<>();
            for (List<Object> combo : columnValueCombinations) {
                for (PivotField valField : valFields) {
                    PivotHeaderCell cell = new PivotHeaderCell();
                    cell.setValue(valField.getDisplayName() != null ? valField.getDisplayName() : valField.getFieldName());
                    cell.setFieldName(valField.getFieldName());
                    cell.setFieldValue(buildKey(combo) + DELIMITER + valField.getFieldName());
                    cell.setLevel(colLevelCount);
                    cell.setColSpan(1);
                    cell.setRowSpan(1);
                    cell.setIsLeaf(true);
                    valHeaderRow.add(cell);
                }
            }
            headers.add(valHeaderRow);
        }

        if (colLevelCount == 0) {
            List<PivotHeaderCell> valHeaderRow = new ArrayList<>();
            for (PivotField valField : valFields) {
                PivotHeaderCell cell = new PivotHeaderCell();
                cell.setValue(valField.getDisplayName() != null ? valField.getDisplayName() : valField.getFieldName());
                cell.setFieldName(valField.getFieldName());
                cell.setFieldValue(valField.getFieldName());
                cell.setLevel(0);
                cell.setColSpan(1);
                cell.setRowSpan(1);
                cell.setIsLeaf(true);
                valHeaderRow.add(cell);
            }
            headers.add(valHeaderRow);
        }

        return headers;
    }

    /**
     * 构建多层行表头
     */
    public List<List<PivotHeaderCell>> buildRowHeaders(PivotTableConfig config, List<List<Object>> rowValueCombinations) {
        List<List<PivotHeaderCell>> headers = new ArrayList<>();
        List<PivotField> rowFields = config.getRowFields() != null ? config.getRowFields() : new ArrayList<>();

        int rowLevelCount = rowFields.size();

        for (int rowIdx = 0; rowIdx < rowValueCombinations.size(); rowIdx++) {
            List<PivotHeaderCell> rowHeaders = new ArrayList<>();
            List<Object> combo = rowValueCombinations.get(rowIdx);

            for (int level = 0; level < rowLevelCount; level++) {
                PivotField field = rowFields.get(level);
                Object value = combo.get(level);

                boolean shouldShow = true;
                int rowSpan = 1;

                if (rowIdx > 0) {
                    List<Object> prevCombo = rowValueCombinations.get(rowIdx - 1);
                    boolean sameAsPrev = true;
                    for (int l = 0; l <= level; l++) {
                        if (!Objects.equals(combo.get(l), prevCombo.get(l))) {
                            sameAsPrev = false;
                            break;
                        }
                    }
                    if (sameAsPrev) {
                        shouldShow = false;
                    }
                }

                if (shouldShow) {
                    for (int i = rowIdx + 1; i < rowValueCombinations.size(); i++) {
                        List<Object> nextCombo = rowValueCombinations.get(i);
                        boolean sameAsNext = true;
                        for (int l = 0; l <= level; l++) {
                            if (!Objects.equals(combo.get(l), nextCombo.get(l))) {
                                sameAsNext = false;
                                break;
                            }
                        }
                        if (sameAsNext) {
                            rowSpan++;
                        } else {
                            break;
                        }
                    }
                }

                PivotHeaderCell cell = new PivotHeaderCell();
                cell.setValue(shouldShow ? (value == null ? "" : value) : null);
                cell.setFieldValue(value);
                cell.setFieldName(field.getFieldName());
                cell.setLevel(level);
                cell.setRowSpan(shouldShow ? rowSpan : 0);
                cell.setColSpan(1);
                cell.setIsLeaf(level == rowLevelCount - 1);
                rowHeaders.add(cell);
            }

            headers.add(rowHeaders);
        }

        return headers;
    }

    /**
     * 构建数据矩阵
     */
    public List<List<PivotDataCell>> buildDataMatrix(PivotTableConfig config, Map<String, Map<String, Object>> dataMap,
                                                      List<List<Object>> rowCombos, List<List<Object>> colCombos) {
        List<List<PivotDataCell>> dataMatrix = new ArrayList<>();
        List<PivotField> valFields = config.getValueFields() != null ? config.getValueFields() : new ArrayList<>();

        for (int rowIdx = 0; rowIdx < rowCombos.size(); rowIdx++) {
            List<PivotDataCell> rowData = new ArrayList<>();
            String rowKey = buildKey(rowCombos.get(rowIdx));
            Map<String, Object> rowDataMap = dataMap.get(rowKey);

            for (int colIdx = 0; colIdx < colCombos.size(); colIdx++) {
                String colKey = buildKey(colCombos.get(colIdx));

                for (int valIdx = 0; valIdx < valFields.size(); valIdx++) {
                    PivotField valField = valFields.get(valIdx);
                    String cellKey = colKey + DELIMITER + valField.getFieldName();

                    PivotDataCell cell = new PivotDataCell();
                    cell.setRowIndex(rowIdx);
                    cell.setColIndex(colIdx * valFields.size() + valIdx);
                    cell.setAggregateFunction(StringUtils.defaultIfBlank(valField.getAggregateFunction(), "SUM"));
                    cell.setIsSubtotal(false);
                    cell.setIsGrandTotal(false);

                    if (rowDataMap != null && rowDataMap.containsKey(cellKey)) {
                        Object value = rowDataMap.get(cellKey);
                        cell.setValue(value);
                        cell.setFormattedValue(value != null ? value.toString() : "");
                    } else {
                        cell.setValue(null);
                        cell.setFormattedValue("");
                    }

                    rowData.add(cell);
                }
            }

            dataMatrix.add(rowData);
        }

        return dataMatrix;
    }

    /**
     * 计算小计和总计
     */
    public void calculateSubtotalsAndTotals(PivotTableResult result, Map<String, Map<String, Object>> dataMap,
                                            List<List<Object>> rowCombos, List<List<Object>> colCombos,
                                            PivotTableConfig config) {
        List<PivotField> valFields = config.getValueFields() != null ? config.getValueFields() : new ArrayList<>();

        if (Boolean.TRUE.equals(config.getShowGrandTotal())) {
            Map<String, Object> grandTotalMap = new HashMap<>();
            for (List<Object> colCombo : colCombos) {
                String colKey = buildKey(colCombo);
                for (PivotField valField : valFields) {
                    String cellKey = colKey + DELIMITER + valField.getFieldName();
                    String aggFunc = StringUtils.defaultIfBlank(valField.getAggregateFunction(), "SUM");
                    Object total = calculateAggregate(dataMap, cellKey, aggFunc);
                    grandTotalMap.put(cellKey, total);
                }
            }

            if (result.getSummary() == null) {
                result.setSummary(new HashMap<>());
            }
            result.getSummary().put("grandTotal", grandTotalMap);
        }

        if (Boolean.TRUE.equals(config.getShowSubtotal()) && config.getRowFields() != null && config.getRowFields().size() > 1) {
            Map<String, Map<String, Object>> subtotalMap = new HashMap<>();

            for (int level = 0; level < config.getRowFields().size() - 1; level++) {
                Map<Object, List<String>> groupMap = new LinkedHashMap<>();

                for (List<Object> rowCombo : rowCombos) {
                    Object groupKey = rowCombo.get(level);
                    String rowKey = buildKey(rowCombo);
                    groupMap.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(rowKey);
                }

                for (Map.Entry<Object, List<String>> entry : groupMap.entrySet()) {
                    Map<String, Object> levelTotal = new HashMap<>();
                    for (List<Object> colCombo : colCombos) {
                        String colKey = buildKey(colCombo);
                        for (PivotField valField : valFields) {
                            String cellKey = colKey + DELIMITER + valField.getFieldName();
                            String aggFunc = StringUtils.defaultIfBlank(valField.getAggregateFunction(), "SUM");
                            Object total = calculateAggregateForRows(dataMap, entry.getValue(), cellKey, aggFunc);
                            levelTotal.put(cellKey, total);
                        }
                    }
                    subtotalMap.put(level + DELIMITER + entry.getKey(), levelTotal);
                }
            }

            if (result.getSummary() == null) {
                result.setSummary(new HashMap<>());
            }
            result.getSummary().put("subtotals", subtotalMap);
        }
    }

    /**
     * 从SQL提取主表名
     */
    public String extractMainTableName(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        Pattern tablePattern = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = tablePattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String buildKey(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(v -> v == null ? "" : v.toString())
                .collect(Collectors.joining(DELIMITER));
    }

    private Object calculateAggregate(Map<String, Map<String, Object>> dataMap, String cellKey, String aggFunc) {
        List<Object> values = new ArrayList<>();
        for (Map<String, Object> rowMap : dataMap.values()) {
            if (rowMap.containsKey(cellKey)) {
                values.add(rowMap.get(cellKey));
            }
        }
        return calculateAggregate(values, aggFunc);
    }

    private Object calculateAggregateForRows(Map<String, Map<String, Object>> dataMap, List<String> rowKeys,
                                              String cellKey, String aggFunc) {
        List<Object> values = new ArrayList<>();
        for (String rowKey : rowKeys) {
            Map<String, Object> rowMap = dataMap.get(rowKey);
            if (rowMap != null && rowMap.containsKey(cellKey)) {
                values.add(rowMap.get(cellKey));
            }
        }
        return calculateAggregate(values, aggFunc);
    }

    private Object calculateAggregate(List<Object> values, String aggFunc) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<Number> numbers = values.stream()
                .filter(v -> v instanceof Number)
                .map(v -> (Number) v)
                .collect(Collectors.toList());

        if (numbers.isEmpty()) {
            return null;
        }

        switch (aggFunc.toUpperCase()) {
            case "SUM":
                return sumNumbers(numbers);
            case "AVG":
                double sum = sumNumbers(numbers).doubleValue();
                return sum / numbers.size();
            case "COUNT":
                return numbers.size();
            case "MAX":
                return numbers.stream()
                        .max(Comparator.comparingDouble(Number::doubleValue))
                        .orElse(null);
            case "MIN":
                return numbers.stream()
                        .min(Comparator.comparingDouble(Number::doubleValue))
                        .orElse(null);
            default:
                return sumNumbers(numbers);
        }
    }

    private Number sumNumbers(List<Number> numbers) {
        if (numbers.stream().allMatch(n -> n instanceof Integer || n instanceof Long)) {
            long sum = 0;
            for (Number n : numbers) {
                sum += n.longValue();
            }
            return sum;
        } else {
            double sum = 0;
            for (Number n : numbers) {
                sum += n.doubleValue();
            }
            return sum;
        }
    }
}
