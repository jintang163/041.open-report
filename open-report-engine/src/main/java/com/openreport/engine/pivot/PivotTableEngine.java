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

public class PivotTableEngine {

    private static final String DELIMITER = "||";
    private static final String SUBTOTAL_LABEL = "小计";
    private static final String GRAND_TOTAL_LABEL = "总计";

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

        List<List<Object>> baseRowCombos = new ArrayList<>(rowValueSet);
        List<List<Object>> colCombos = new ArrayList<>(colValueSet);

        Map<String, Map<String, Object>> allDataMap = new HashMap<>(dataMap);

        List<RowEntry> rowEntries = buildRowEntries(baseRowCombos, rowFields, config, allDataMap, colCombos, valFields);

        List<List<Object>> allRowCombos = rowEntries.stream()
                .map(RowEntry::getValues)
                .collect(Collectors.toList());

        List<List<PivotHeaderCell>> rowHeaders = buildRowHeadersWithSubtotals(rowEntries, rowFields);
        List<List<PivotHeaderCell>> columnHeaders = buildColumnHeaders(config, colCombos);
        List<List<PivotDataCell>> dataCells = buildDataMatrixWithSubtotals(rowEntries, config, allDataMap, colCombos);

        result.setRowHeaders(rowHeaders);
        result.setColumnHeaders(columnHeaders);
        result.setDataCells(dataCells);

        Map<String, Object> summary = new HashMap<>();
        summary.put("rowCount", baseRowCombos.size());
        summary.put("columnCount", colCombos.size() * valFields.size());
        summary.put("dataCount", rawData.size());
        result.setSummary(summary);

        List<String> drillDownFields = rowFields.stream()
                .map(PivotField::getFieldName)
                .collect(Collectors.toList());
        result.setDrillDownFields(drillDownFields);

        return result;
    }

    private List<RowEntry> buildRowEntries(List<List<Object>> baseRowCombos, List<PivotField> rowFields,
                                            PivotTableConfig config, Map<String, Map<String, Object>> dataMap,
                                            List<List<Object>> colCombos, List<PivotField> valFields) {
        List<RowEntry> entries = new ArrayList<>();
        boolean showSubtotal = Boolean.TRUE.equals(config.getShowSubtotal()) && rowFields.size() > 1;
        boolean showGrandTotal = Boolean.TRUE.equals(config.getShowGrandTotal());
        boolean subtotalTop = "top".equalsIgnoreCase(config.getSubtotalPosition());

        if (rowFields.isEmpty()) {
            for (List<Object> combo : baseRowCombos) {
                entries.add(new RowEntry(combo, RowEntryType.DATA, -1, null));
            }
            if (showGrandTotal) {
                List<Object> totalValues = Collections.nCopies(1, (Object) GRAND_TOTAL_LABEL);
                Map<String, Object> totalData = calculateTotalForRows(dataMap,
                        baseRowCombos.stream().map(this::buildKey).collect(Collectors.toList()),
                        colCombos, valFields);
                String totalKey = buildKey(totalValues);
                dataMap.put(totalKey, totalData);
                entries.add(new RowEntry(totalValues, RowEntryType.GRAND_TOTAL, -1, null));
            }
            return entries;
        }

        Map<Object, List<List<Object>>> groupMap = new LinkedHashMap<>();
        for (List<Object> combo : baseRowCombos) {
            Object groupKey = combo.get(0);
            groupMap.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(combo);
        }

        for (Map.Entry<Object, List<List<Object>>> groupEntry : groupMap.entrySet()) {
            List<List<Object>> groupCombos = groupEntry.getValue();
            Object groupKey = groupEntry.getKey();

            if (showSubtotal && subtotalTop) {
                List<Object> subtotalValues = new ArrayList<>();
                subtotalValues.add(groupKey);
                for (int i = 1; i < rowFields.size(); i++) {
                    subtotalValues.add(SUBTOTAL_LABEL);
                }
                Map<String, Object> subtotalData = calculateTotalForRows(dataMap,
                        groupCombos.stream().map(this::buildKey).collect(Collectors.toList()),
                        colCombos, valFields);
                String subtotalKey = buildKey(subtotalValues);
                dataMap.put(subtotalKey, subtotalData);
                entries.add(new RowEntry(subtotalValues, RowEntryType.SUBTOTAL, 0, groupKey));
            }

            for (int i = 0; i < groupCombos.size(); i++) {
                List<Object> combo = groupCombos.get(i);
                entries.add(new RowEntry(combo, RowEntryType.DATA, -1, null));
            }

            if (showSubtotal && !subtotalTop) {
                List<Object> subtotalValues = new ArrayList<>();
                subtotalValues.add(groupKey);
                for (int i = 1; i < rowFields.size(); i++) {
                    subtotalValues.add(SUBTOTAL_LABEL);
                }
                Map<String, Object> subtotalData = calculateTotalForRows(dataMap,
                        groupCombos.stream().map(this::buildKey).collect(Collectors.toList()),
                        colCombos, valFields);
                String subtotalKey = buildKey(subtotalValues);
                dataMap.put(subtotalKey, subtotalData);
                entries.add(new RowEntry(subtotalValues, RowEntryType.SUBTOTAL, 0, groupKey));
            }
        }

        if (showGrandTotal) {
            List<Object> totalValues = new ArrayList<>();
            totalValues.add(GRAND_TOTAL_LABEL);
            for (int i = 1; i < rowFields.size(); i++) {
                totalValues.add("");
            }
            Map<String, Object> totalData = calculateTotalForRows(dataMap,
                    baseRowCombos.stream().map(this::buildKey).collect(Collectors.toList()),
                    colCombos, valFields);
            String totalKey = buildKey(totalValues);
            dataMap.put(totalKey, totalData);
            entries.add(new RowEntry(totalValues, RowEntryType.GRAND_TOTAL, -1, null));
        }

        return entries;
    }

    private Map<String, Object> calculateTotalForRows(Map<String, Map<String, Object>> dataMap,
                                                       List<String> rowKeys, List<List<Object>> colCombos,
                                                       List<PivotField> valFields) {
        Map<String, Object> result = new HashMap<>();
        for (List<Object> colCombo : colCombos) {
            String colKey = buildKey(colCombo);
            for (PivotField valField : valFields) {
                String cellKey = colKey + DELIMITER + valField.getFieldName();
                String aggFunc = StringUtils.defaultIfBlank(valField.getAggregateFunction(), "SUM");
                List<Object> values = new ArrayList<>();
                for (String rowKey : rowKeys) {
                    Map<String, Object> rowData = dataMap.get(rowKey);
                    if (rowData != null && rowData.containsKey(cellKey)) {
                        values.add(rowData.get(cellKey));
                    }
                }
                Object aggregated = calculateAggregate(values, aggFunc);
                result.put(cellKey, aggregated);
            }
        }
        return result;
    }

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
                cell.setValue(entry.getKey() == null ? "" : entry.getKey());
                cell.setFieldValue(entry.getKey());
                cell.setFieldName(field.getFieldName());
                cell.setLevel(level);
                cell.setColSpan(entry.getValue());
                cell.setRowSpan(1);
                cell.setIsLeaf(level == colLevelCount - 1 && valCount <= 1);
                levelHeaders.add(cell);
            }

            headers.add(levelHeaders);
        }

        if (valCount > 1 || colLevelCount == 0) {
            List<PivotHeaderCell> valHeaderRow = new ArrayList<>();
            if (colLevelCount > 0) {
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
            } else {
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
            }
            headers.add(valHeaderRow);
        }

        return headers;
    }

    public List<List<PivotHeaderCell>> buildRowHeadersWithSubtotals(List<RowEntry> rowEntries, List<PivotField> rowFields) {
        List<List<PivotHeaderCell>> headers = new ArrayList<>();
        int rowLevelCount = rowFields.isEmpty() ? 1 : rowFields.size();

        for (int rowIdx = 0; rowIdx < rowEntries.size(); rowIdx++) {
            RowEntry entry = rowEntries.get(rowIdx);
            List<Object> combo = entry.getValues();
            RowEntryType type = entry.getType();

            List<PivotHeaderCell> rowHeaderCells = new ArrayList<>();

            if (type == RowEntryType.GRAND_TOTAL) {
                PivotHeaderCell cell = new PivotHeaderCell();
                cell.setValue(combo.get(0));
                cell.setFieldValue(combo.get(0));
                cell.setFieldName("__grand_total__");
                cell.setLevel(0);
                cell.setRowSpan(1);
                cell.setColSpan(rowLevelCount);
                cell.setIsLeaf(true);
                rowHeaderCells.add(cell);
                for (int l = 1; l < rowLevelCount; l++) {
                    PivotHeaderCell emptyCell = new PivotHeaderCell();
                    emptyCell.setValue(null);
                    emptyCell.setRowSpan(0);
                    emptyCell.setColSpan(0);
                    rowHeaderCells.add(emptyCell);
                }
                headers.add(rowHeaderCells);
                continue;
            }

            if (type == RowEntryType.SUBTOTAL) {
                int subtotalLevel = entry.getSubtotalLevel();
                PivotHeaderCell firstCell = new PivotHeaderCell();
                firstCell.setValue(combo.get(subtotalLevel));
                firstCell.setFieldValue(combo.get(subtotalLevel));
                firstCell.setFieldName(rowFields.get(subtotalLevel).getFieldName());
                firstCell.setLevel(subtotalLevel);
                firstCell.setRowSpan(1);
                firstCell.setColSpan(rowLevelCount - subtotalLevel);
                firstCell.setIsLeaf(true);
                rowHeaderCells.add(firstCell);

                for (int l = 1; l < rowLevelCount; l++) {
                    PivotHeaderCell emptyCell = new PivotHeaderCell();
                    emptyCell.setValue(null);
                    emptyCell.setRowSpan(0);
                    emptyCell.setColSpan(0);
                    rowHeaderCells.add(emptyCell);
                }
                headers.add(rowHeaderCells);
                continue;
            }

            for (int level = 0; level < rowLevelCount; level++) {
                PivotField field = rowFields.get(level);
                Object value = combo.size() > level ? combo.get(level) : null;

                boolean shouldShow = true;
                int rowSpan = 1;

                if (rowIdx > 0) {
                    RowEntry prevEntry = rowEntries.get(rowIdx - 1);
                    if (prevEntry.getType() == RowEntryType.DATA) {
                        List<Object> prevCombo = prevEntry.getValues();
                        boolean sameAsPrev = true;
                        for (int l = 0; l <= level; l++) {
                            Object currVal = combo.size() > l ? combo.get(l) : null;
                            Object prevVal = prevCombo.size() > l ? prevCombo.get(l) : null;
                            if (!Objects.equals(currVal, prevVal)) {
                                sameAsPrev = false;
                                break;
                            }
                        }
                        if (sameAsPrev) {
                            shouldShow = false;
                        }
                    }
                }

                if (shouldShow) {
                    for (int i = rowIdx + 1; i < rowEntries.size(); i++) {
                        RowEntry nextEntry = rowEntries.get(i);
                        if (nextEntry.getType() != RowEntryType.DATA) {
                            break;
                        }
                        List<Object> nextCombo = nextEntry.getValues();
                        boolean sameAsNext = true;
                        for (int l = 0; l <= level; l++) {
                            Object currVal = combo.size() > l ? combo.get(l) : null;
                            Object nextVal = nextCombo.size() > l ? nextCombo.get(l) : null;
                            if (!Objects.equals(currVal, nextVal)) {
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
                rowHeaderCells.add(cell);
            }

            headers.add(rowHeaderCells);
        }

        return headers;
    }

    public List<List<PivotDataCell>> buildDataMatrixWithSubtotals(List<RowEntry> rowEntries, PivotTableConfig config,
                                                                   Map<String, Map<String, Object>> dataMap,
                                                                   List<List<Object>> colCombos) {
        List<List<PivotDataCell>> dataMatrix = new ArrayList<>();
        List<PivotField> valFields = config.getValueFields() != null ? config.getValueFields() : new ArrayList<>();

        for (int rowIdx = 0; rowIdx < rowEntries.size(); rowIdx++) {
            RowEntry entry = rowEntries.get(rowIdx);
            List<PivotDataCell> rowData = new ArrayList<>();
            String rowKey = buildKey(entry.getValues());
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
                    cell.setIsSubtotal(entry.getType() == RowEntryType.SUBTOTAL);
                    cell.setIsGrandTotal(entry.getType() == RowEntryType.GRAND_TOTAL);

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

    public void calculateSubtotalsAndTotals(PivotTableResult result, Map<String, Map<String, Object>> dataMap,
                                            List<List<Object>> rowCombos, List<List<Object>> colCombos,
                                            PivotTableConfig config) {
    }

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

    private enum RowEntryType {
        DATA,
        SUBTOTAL,
        GRAND_TOTAL
    }

    private static class RowEntry {
        private final List<Object> values;
        private final RowEntryType type;
        private final int subtotalLevel;
        private final Object groupKey;

        public RowEntry(List<Object> values, RowEntryType type, int subtotalLevel, Object groupKey) {
            this.values = values;
            this.type = type;
            this.subtotalLevel = subtotalLevel;
            this.groupKey = groupKey;
        }

        public List<Object> getValues() {
            return values;
        }

        public RowEntryType getType() {
            return type;
        }

        public int getSubtotalLevel() {
            return subtotalLevel;
        }

        public Object getGroupKey() {
            return groupKey;
        }
    }
}
