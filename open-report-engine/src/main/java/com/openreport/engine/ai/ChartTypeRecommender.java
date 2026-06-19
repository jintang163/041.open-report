package com.openreport.engine.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChartTypeRecommender {

    public enum FieldCategory {
        DIMENSION,
        MEASURE,
        DATE,
        GEO,
        UNKNOWN
    }

    public enum ChartType {
        BAR,
        LINE,
        PIE,
        RADAR,
        SCATTER,
        TABLE,
        KPI_CARD,
        AREA,
        FUNNEL
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldInfo {
        private String name;
        private String type;
        private FieldCategory category;
        private Integer distinctCount;
        private Object sampleValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationResult {
        private ChartType recommendedType;
        private String reason;
        private List<ChartType> alternatives;
        private List<String> xFieldCandidates;
        private List<String> yFieldCandidates;
        private Integer suggestedCardWidth;
        private Integer suggestedCardHeight;
    }

    public RecommendationResult recommend(List<FieldInfo> fields) {
        if (fields == null || fields.isEmpty()) {
            RecommendationResult result = new RecommendationResult();
            result.setRecommendedType(ChartType.TABLE);
            result.setReason("无可用字段，默认展示表格");
            result.setAlternatives(new ArrayList<>());
            result.setXFieldCandidates(new ArrayList<>());
            result.setYFieldCandidates(new ArrayList<>());
            result.setSuggestedCardWidth(800);
            result.setSuggestedCardHeight(400);
            return result;
        }

        List<FieldInfo> dimensions = fields.stream()
                .filter(f -> f.getCategory() == FieldCategory.DIMENSION || f.getCategory() == FieldCategory.GEO)
                .collect(Collectors.toList());
        List<FieldInfo> measures = fields.stream()
                .filter(f -> f.getCategory() == FieldCategory.MEASURE)
                .collect(Collectors.toList());
        List<FieldInfo> dates = fields.stream()
                .filter(f -> f.getCategory() == FieldCategory.DATE)
                .collect(Collectors.toList());
        int dimCount = dimensions.size() + dates.size();
        int measureCount = measures.size();

        List<FieldInfo> xFields = new ArrayList<>();
        if (!dates.isEmpty()) {
            xFields.addAll(dates);
        }
        xFields.addAll(dimensions);

        List<ChartType> alternatives = new ArrayList<>();
        ChartType recommended;
        String reason;

        if (measureCount == 0 && dimCount == 1) {
            FieldInfo singleDim = dates.isEmpty() ? dimensions.get(0) : dates.get(0);
            recommended = ChartType.TABLE;
            reason = String.format("单维度字段 '%s'，建议表格展示", singleDim.getName());
            alternatives.add(ChartType.TABLE);
        } else if (measureCount == 1 && dimCount == 0) {
            FieldInfo measure = measures.get(0);
            recommended = ChartType.KPI_CARD;
            reason = String.format("单度量字段 '%s'，建议 KPI 卡片展示", measure.getName());
            alternatives.add(ChartType.KPI_CARD);
            alternatives.add(ChartType.TABLE);
        } else if (measureCount == 1 && dimCount == 1) {
            FieldInfo dim = xFields.get(0);
            FieldInfo measure = measures.get(0);

            if (dim.getCategory() == FieldCategory.DATE || "date".equalsIgnoreCase(dim.getType()) || "datetime".equalsIgnoreCase(dim.getType())) {
                recommended = ChartType.LINE;
                reason = String.format("日期维度 '%s' + 度量 '%s'，建议折线图展示趋势", dim.getName(), measure.getName());
                alternatives.add(ChartType.LINE);
                alternatives.add(ChartType.AREA);
                alternatives.add(ChartType.BAR);
            } else if (dim.getDistinctCount() != null && dim.getDistinctCount() <= 8) {
                recommended = ChartType.PIE;
                reason = String.format("维度 '%s'（%d个分类） + 度量 '%s'，建议饼图展示占比", dim.getName(), dim.getDistinctCount(), measure.getName());
                alternatives.add(ChartType.PIE);
                alternatives.add(ChartType.BAR);
                alternatives.add(ChartType.RADAR);
            } else if (dim.getDistinctCount() != null && dim.getDistinctCount() <= 15) {
                recommended = ChartType.BAR;
                reason = String.format("维度 '%s'（%d个分类） + 度量 '%s'，建议柱状图比较", dim.getName(), dim.getDistinctCount(), measure.getName());
                alternatives.add(ChartType.BAR);
                alternatives.add(ChartType.PIE);
                alternatives.add(ChartType.TABLE);
            } else {
                recommended = ChartType.BAR;
                reason = String.format("维度 '%s'（较多分类） + 度量 '%s'，建议柱状图", dim.getName(), measure.getName());
                alternatives.add(ChartType.BAR);
                alternatives.add(ChartType.TABLE);
                alternatives.add(ChartType.LINE);
            }
        } else if (measureCount == 1 && dimCount >= 2) {
            recommended = ChartType.RADAR;
            reason = String.format("多维分析（%d维度 + %d度量），建议雷达图综合比较", dimCount, measureCount);
            alternatives.add(ChartType.RADAR);
            alternatives.add(ChartType.BAR);
            alternatives.add(ChartType.TABLE);
        } else if (measureCount >= 2 && dimCount == 1) {
            FieldInfo dim = xFields.get(0);
            if (dim.getCategory() == FieldCategory.DATE) {
                recommended = ChartType.LINE;
                reason = String.format("日期维度 '%s' + %d个度量，建议折线图多系列对比趋势", dim.getName(), measureCount);
                alternatives.add(ChartType.LINE);
                alternatives.add(ChartType.AREA);
                alternatives.add(ChartType.BAR);
            } else {
                recommended = ChartType.BAR;
                reason = String.format("维度 '%s' + %d个度量，建议柱状图多系列对比", dim.getName(), measureCount);
                alternatives.add(ChartType.BAR);
                alternatives.add(ChartType.LINE);
                alternatives.add(ChartType.RADAR);
            }
        } else if (measureCount >= 2 && dimCount >= 2) {
            recommended = ChartType.SCATTER;
            reason = String.format("%d维度 + %d度量，建议散点图查看相关性", dimCount, measureCount);
            alternatives.add(ChartType.SCATTER);
            alternatives.add(ChartType.BAR);
            alternatives.add(ChartType.TABLE);
            alternatives.add(ChartType.RADAR);
        } else if (measureCount == 0 && dimCount >= 2) {
            recommended = ChartType.TABLE;
            reason = String.format("%d个维度字段，建议表格明细展示", dimCount);
            alternatives.add(ChartType.TABLE);
        } else {
            recommended = ChartType.TABLE;
            reason = String.format("%d个字段，默认表格展示", fields.size());
            alternatives.add(ChartType.TABLE);
            alternatives.add(ChartType.BAR);
        }

        int cardWidth;
        int cardHeight;
        switch (recommended) {
            case KPI_CARD:
                cardWidth = 300;
                cardHeight = 180;
                break;
            case PIE:
            case RADAR:
                cardWidth = 420;
                cardHeight = 380;
                break;
            case SCATTER:
                cardWidth = 480;
                cardHeight = 360;
                break;
            case TABLE:
                cardWidth = 800;
                cardHeight = 400;
                break;
            default:
                cardWidth = 500;
                cardHeight = 340;
                break;
        }

        RecommendationResult result = new RecommendationResult();
        result.setRecommendedType(recommended);
        result.setReason(reason);
        result.setAlternatives(alternatives);
        result.setXFieldCandidates(xFields.stream().map(FieldInfo::getName).collect(Collectors.toList()));
        result.setYFieldCandidates(measures.stream().map(FieldInfo::getName).collect(Collectors.toList()));
        result.setSuggestedCardWidth(cardWidth);
        result.setSuggestedCardHeight(cardHeight);

        log.debug("Chart recommendation: fields={}, recommended={}, reason={}",
                fields.size(), recommended, reason);

        return result;
    }

    public FieldCategory categorizeField(String fieldName, String fieldType, Object sampleValue) {
        String lowerType = fieldType != null ? fieldType.toLowerCase() : "";
        String lowerName = fieldName != null ? fieldName.toLowerCase() : "";

        if (lowerType.contains("date") || lowerType.contains("time") || lowerType.contains("year")
                || lowerType.contains("month") || lowerType.contains("day")) {
            return FieldCategory.DATE;
        }

        if (lowerType.contains("int") || lowerType.contains("bigint") || lowerType.contains("decimal")
                || lowerType.contains("double") || lowerType.contains("float") || lowerType.contains("number")
                || lowerType.contains("numeric")) {
            if (lowerName.contains("id") || lowerName.contains("code")) {
                return FieldCategory.DIMENSION;
            }
            if (lowerName.contains("amount") || lowerName.contains("total") || lowerName.contains("sum")
                    || lowerName.contains("count") || lowerName.contains("price") || lowerName.contains("sales")
                    || lowerName.contains("quantity") || lowerName.contains("profit") || lowerName.contains("revenue")) {
                return FieldCategory.MEASURE;
            }
            if (sampleValue instanceof Number) {
                Number num = (Number) sampleValue;
                if (num.doubleValue() >= 0 && num.doubleValue() <= 100 && lowerName.contains("rate")) {
                    return FieldCategory.MEASURE;
                }
                if (num.doubleValue() > 10000) {
                    return FieldCategory.MEASURE;
                }
            }
            return FieldCategory.MEASURE;
        }

        if (lowerType.contains("string") || lowerType.contains("varchar") || lowerType.contains("char")
                || lowerType.contains("text")) {
            if (lowerName.contains("province") || lowerName.contains("city") || lowerName.contains("region")
                    || lowerName.contains("country") || lowerName.contains("area") || lowerName.contains("address")
                    || lowerName.contains("location")) {
                return FieldCategory.GEO;
            }
            if (lowerName.contains("id") || lowerName.contains("code") || lowerName.contains("name")
                    || lowerName.contains("type") || lowerName.contains("category") || lowerName.contains("class")
                    || lowerName.contains("status") || lowerName.contains("level")) {
                return FieldCategory.DIMENSION;
            }
            return FieldCategory.DIMENSION;
        }

        if (lowerType.contains("bool")) {
            return FieldCategory.DIMENSION;
        }

        return FieldCategory.UNKNOWN;
    }

    public List<FieldInfo> analyzeFields(List<Map<String, Object>> sampleData, List<Map<String, String>> columnMeta) {
        List<FieldInfo> result = new ArrayList<>();

        if (sampleData == null || sampleData.isEmpty() || columnMeta == null) {
            return result;
        }

        for (Map<String, String> col : columnMeta) {
            String name = col.get("name");
            String type = col.get("type");
            if (name == null) continue;

            Set<Object> distinctValues = new HashSet<>();
            Object sampleVal = null;
            for (Map<String, Object> row : sampleData) {
                Object val = row.get(name);
                if (sampleVal == null && val != null) {
                    sampleVal = val;
                }
                if (val != null) {
                    distinctValues.add(val);
                }
                if (distinctValues.size() > 100) {
                    break;
                }
            }

            FieldCategory category = categorizeField(name, type, sampleVal);
            FieldInfo info = new FieldInfo();
            info.setName(name);
            info.setType(type);
            info.setCategory(category);
            info.setDistinctCount(distinctValues.size());
            info.setSampleValue(sampleVal);
            result.add(info);
        }

        return result;
    }
}
