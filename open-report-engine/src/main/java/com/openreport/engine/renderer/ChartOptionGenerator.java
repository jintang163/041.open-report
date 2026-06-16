package com.openreport.engine.renderer;

import com.openreport.common.exception.BusinessException;
import com.openreport.engine.model.ChartConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ChartOptionGenerator {

    public Map<String, Object> generateOption(ChartConfig config, List<Map<String, Object>> data) {
        String chartType = config.getChartType();
        if (chartType == null || chartType.isEmpty()) {
            throw new BusinessException("Chart type is required");
        }

        switch (chartType.toLowerCase()) {
            case "line":
                return generateLineOption(config, data);
            case "bar":
                return generateBarOption(config, data);
            case "pie":
                return generatePieOption(config, data);
            case "scatter":
                return generateScatterOption(config, data);
            case "radar":
                return generateRadarOption(config, data);
            case "gauge":
                return generateGaugeOption(config, data);
            default:
                return generateLineOption(config, data);
        }
    }

    private Map<String, Object> generateLineOption(ChartConfig config, List<Map<String, Object>> data) {
        Map<String, Object> option = buildBaseOption(config);

        List<Object> xAxisData = new ArrayList<>();
        List<Map<String, Object>> series = new ArrayList<>();

        if (config.getXAxisField() != null && config.getYAxisFields() != null) {
            for (Map<String, Object> row : data) {
                xAxisData.add(row.get(config.getXAxisField()));
            }

            for (String yField : config.getYAxisFields()) {
                Map<String, Object> seriesItem = new LinkedHashMap<>();
                seriesItem.put("name", yField);
                seriesItem.put("type", "line");
                List<Object> seriesData = new ArrayList<>();
                for (Map<String, Object> row : data) {
                    seriesData.add(row.get(yField));
                }
                seriesItem.put("data", seriesData);
                series.add(seriesItem);
            }
        }

        Map<String, Object> xAxis = new LinkedHashMap<>();
        xAxis.put("type", "category");
        xAxis.put("data", xAxisData);
        option.put("xAxis", xAxis);

        Map<String, Object> yAxis = new LinkedHashMap<>();
        yAxis.put("type", "value");
        option.put("yAxis", yAxis);

        option.put("series", series);
        return option;
    }

    private Map<String, Object> generateBarOption(ChartConfig config, List<Map<String, Object>> data) {
        Map<String, Object> option = generateLineOption(config, data);
        List<Map<String, Object>> series = (List<Map<String, Object>>) option.get("series");
        if (series != null) {
            for (Map<String, Object> seriesItem : series) {
                seriesItem.put("type", "bar");
            }
        }
        return option;
    }

    private Map<String, Object> generatePieOption(ChartConfig config, List<Map<String, Object>> data) {
        Map<String, Object> option = buildBaseOption(config);

        List<Map<String, Object>> pieData = new ArrayList<>();
        if (config.getXAxisField() != null && config.getYAxisFields() != null && !config.getYAxisFields().isEmpty()) {
            String valueField = config.getYAxisFields().get(0);
            for (Map<String, Object> row : data) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", row.get(config.getXAxisField()));
                item.put("value", row.get(valueField));
                pieData.add(item);
            }
        }

        Map<String, Object> seriesItem = new LinkedHashMap<>();
        seriesItem.put("name", config.getTitle());
        seriesItem.put("type", "pie");
        seriesItem.put("radius", "50%");
        seriesItem.put("data", pieData);

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(seriesItem);
        option.put("series", series);

        return option;
    }

    private Map<String, Object> generateScatterOption(ChartConfig config, List<Map<String, Object>> data) {
        Map<String, Object> option = buildBaseOption(config);

        List<List<Object>> scatterData = new ArrayList<>();
        if (config.getXAxisField() != null && config.getYAxisFields() != null && !config.getYAxisFields().isEmpty()) {
            String yField = config.getYAxisFields().get(0);
            for (Map<String, Object> row : data) {
                List<Object> point = new ArrayList<>();
                point.add(row.get(config.getXAxisField()));
                point.add(row.get(yField));
                scatterData.add(point);
            }
        }

        Map<String, Object> xAxis = new LinkedHashMap<>();
        xAxis.put("type", "value");
        option.put("xAxis", xAxis);

        Map<String, Object> yAxis = new LinkedHashMap<>();
        yAxis.put("type", "value");
        option.put("yAxis", yAxis);

        Map<String, Object> seriesItem = new LinkedHashMap<>();
        seriesItem.put("name", config.getTitle());
        seriesItem.put("type", "scatter");
        seriesItem.put("data", scatterData);

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(seriesItem);
        option.put("series", series);

        return option;
    }

    private Map<String, Object> generateRadarOption(ChartConfig config, List<Map<String, Object>> data) {
        Map<String, Object> option = buildBaseOption(config);

        List<Map<String, Object>> indicators = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (config.getXAxisField() != null && config.getYAxisFields() != null && !config.getYAxisFields().isEmpty()) {
            String valueField = config.getYAxisFields().get(0);
            for (Map<String, Object> row : data) {
                Map<String, Object> indicator = new LinkedHashMap<>();
                indicator.put("name", row.get(config.getXAxisField()));
                indicator.put("max", 100);
                indicators.add(indicator);
                values.add(row.get(valueField));
            }
        }

        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("indicator", indicators);
        option.put("radar", radar);

        Map<String, Object> seriesItem = new LinkedHashMap<>();
        seriesItem.put("type", "radar");
        Map<String, Object> dataItem = new LinkedHashMap<>();
        dataItem.put("value", values);
        dataItem.put("name", config.getTitle());
        List<Map<String, Object>> seriesData = new ArrayList<>();
        seriesData.add(dataItem);
        seriesItem.put("data", seriesData);

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(seriesItem);
        option.put("series", series);

        return option;
    }

    private Map<String, Object> generateGaugeOption(ChartConfig config, List<Map<String, Object>> data) {
        Map<String, Object> option = buildBaseOption(config);

        Object value = 0;
        if (config.getYAxisFields() != null && !config.getYAxisFields().isEmpty() && !data.isEmpty()) {
            value = data.get(0).get(config.getYAxisFields().get(0));
        }

        Map<String, Object> seriesItem = new LinkedHashMap<>();
        seriesItem.put("type", "gauge");
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("formatter", "{value}%");
        seriesItem.put("detail", detail);
        Map<String, Object> dataItem = new LinkedHashMap<>();
        dataItem.put("value", value);
        dataItem.put("name", config.getTitle());
        List<Map<String, Object>> seriesData = new ArrayList<>();
        seriesData.add(dataItem);
        seriesItem.put("data", seriesData);

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(seriesItem);
        option.put("series", series);

        return option;
    }

    private Map<String, Object> buildBaseOption(ChartConfig config) {
        Map<String, Object> option = new LinkedHashMap<>();

        if (config.getTitle() != null && !config.getTitle().isEmpty()) {
            Map<String, Object> title = new LinkedHashMap<>();
            title.put("text", config.getTitle());
            title.put("left", "center");
            option.put("title", title);
        }

        Map<String, Object> tooltip = new LinkedHashMap<>();
        tooltip.put("trigger", "item");
        if (config.getTooltip() != null) {
            tooltip.putAll(config.getTooltip());
        }
        option.put("tooltip", tooltip);

        Map<String, Object> legend = new LinkedHashMap<>();
        legend.put("bottom", 10);
        if (config.getLegend() != null) {
            legend.putAll(config.getLegend());
        }
        option.put("legend", legend);

        if (config.getExtra() != null) {
            option.putAll(config.getExtra());
        }

        return option;
    }
}
