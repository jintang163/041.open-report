package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openreport.admin.dto.AiGenerateRequest;
import com.openreport.admin.dto.AiGenerateResult;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.AiService;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.admin.service.ReportGeneratorService;
import com.openreport.admin.service.ReportTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

    @Autowired
    private AiService aiService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratedReportResult generateReport(AiGenerateResult aiResult, Long dsId, Long userId) {
        GeneratedReportResult result = new GeneratedReportResult();

        try {
            if (aiResult.getSql() == null || aiResult.getSql().trim().isEmpty()) {
                result.setMessage("SQL不能为空");
                throw new IllegalArgumentException("SQL不能为空");
            }

            Map<String, Object> validateResult = dataSourceConfigService.validateSql(dsId, aiResult.getSql());
            if (validateResult != null && Boolean.FALSE.equals(validateResult.get("success"))) {
                String errorMsg = String.valueOf(validateResult.getOrDefault("message", "SQL校验失败"));
                result.setMessage("SQL校验失败: " + errorMsg);
                throw new IllegalArgumentException("SQL校验失败: " + errorMsg);
            }

            String reportTitle = aiResult.getReportTitle() != null ? aiResult.getReportTitle() : "AI生成报表";
            String datasetName = reportTitle + "_数据集";

            DataSet dataSet = new DataSet();
            dataSet.setDsId(dsId);
            dataSet.setSetName(datasetName);
            dataSet.setSetCode("ai_ds_" + System.currentTimeMillis());
            dataSet.setSetType(1);
            dataSet.setSqlText(aiResult.getSql());
            dataSet.setDescription("AI自动生成的数据集：" + reportTitle);
            dataSet.setStatus(1);
            dataSet.setCreateBy(userId);
            dataSet.setCreateTime(LocalDateTime.now());
            dataSet.setUpdateBy(userId);
            dataSet.setUpdateTime(LocalDateTime.now());

            if (aiResult.getFields() != null && !aiResult.getFields().isEmpty()) {
                JSONArray fieldConfig = new JSONArray();
                for (AiGenerateResult.FieldInfo field : aiResult.getFields()) {
                    JSONObject f = new JSONObject();
                    f.put("name", field.getName());
                    f.put("type", field.getType());
                    f.put("label", field.getLabel());
                    fieldConfig.add(f);
                }
                dataSet.setFieldConfig(fieldConfig.toJSONString());
            }

            dataSetService.save(dataSet);
            result.setDataSetId(dataSet.getId());
            result.setDataSetName(datasetName);

            ReportTemplate template = new ReportTemplate();
            template.setTemplateName(reportTitle);
            template.setTemplateCode("ai_rpt_" + System.currentTimeMillis());
            template.setTemplateType(1);
            template.setDescription("AI自动生成的报表：" + reportTitle);
            template.setStatus(1);
            template.setCreateBy(userId);
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateBy(userId);
            template.setUpdateTime(LocalDateTime.now());

            String templateJson = buildTemplateJson(aiResult, dataSet);
            template.setTemplateJson(templateJson);

            String dataSetBind = buildDataSetBind(dataSet);
            template.setDataSetBind(dataSetBind);

            reportTemplateService.save(template);
            result.setReportId(template.getId());
            result.setReportName(reportTitle);
            result.setMessage("报表生成成功");

        } catch (Exception e) {
            log.error("自动生成报表失败: {}", e.getMessage(), e);
            result.setMessage("报表生成失败: " + e.getMessage());
            throw new RuntimeException("自动生成报表失败", e);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratedReportResult generateReportFromPrompt(String prompt, Long dsId, Long userId) {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setPrompt(prompt);
        request.setDsId(dsId);

        AiGenerateResult aiResult = aiService.generateReport(request);
        return generateReport(aiResult, dsId, userId);
    }

    private String buildTemplateJson(AiGenerateResult aiResult, DataSet dataSet) {
        JSONObject template = new JSONObject();
        template.put("version", "1.0.0");
        template.put("name", aiResult.getReportTitle());
        template.put("createdAt", System.currentTimeMillis());
        template.put("updatedAt", System.currentTimeMillis());

        JSONArray sheets = new JSONArray();
        JSONObject sheet = new JSONObject();
        sheet.put("name", "Sheet1");
        sheet.put("index", 0);

        JSONArray cells = new JSONArray();

        cells.add(createTitleCell(aiResult.getReportTitle()));

        int rowIdx = 2;
        if (aiResult.getDescription() != null) {
            cells.add(createDescriptionCell(rowIdx, aiResult.getDescription()));
            rowIdx += 2;
        }

        if (aiResult.getFields() != null && !aiResult.getFields().isEmpty()) {
            int colIdx = 0;
            for (AiGenerateResult.FieldInfo field : aiResult.getFields()) {
                cells.add(createHeaderCell(rowIdx, colIdx, field.getLabel() != null ? field.getLabel() : field.getName()));
                cells.add(createDataCell(rowIdx + 1, colIdx, field.getName(), dataSet));
                colIdx++;
            }
        }

        sheet.put("cells", cells);

        JSONObject columnWidths = new JSONObject();
        for (int i = 0; i < (aiResult.getFields() != null ? aiResult.getFields().size() : 5); i++) {
            columnWidths.put(String.valueOf(i), 120);
        }
        sheet.put("columnWidths", columnWidths);

        JSONObject rowHeights = new JSONObject();
        rowHeights.put("0", 40);
        rowHeights.put("1", 30);
        sheet.put("rowHeights", rowHeights);

        sheet.put("mergedCells", new JSONObject());

        sheets.add(sheet);
        template.put("sheets", sheets);

        JSONArray charts = new JSONArray();
        if (aiResult.getCharts() != null && !aiResult.getCharts().isEmpty()) {
            int chartIdx = 0;
            int chartX = 50;
            int chartY = 350;
            for (AiGenerateResult.ChartSuggestion chart : aiResult.getCharts()) {
                JSONObject chartJson = new JSONObject();
                chartJson.put("id", "chart_" + chartIdx);
                chartJson.put("type", chart.getChartType());
                chartJson.put("title", chart.getTitle());
                chartJson.put("datasetId", dataSet.getId());
                chartJson.put("datasetName", dataSet.getSetName());
                chartJson.put("xAxisField", chart.getxField());
                chartJson.put("yAxisFields", chart.getYFields());
                chartJson.put("x", chartX);
                chartJson.put("y", chartY);
                chartJson.put("width", 400);
                chartJson.put("height", 300);
                charts.add(chartJson);

                chartX += 430;
                if (chartIdx % 2 == 1) {
                    chartX = 50;
                    chartY += 330;
                }
                chartIdx++;
            }
        }
        template.put("charts", charts);

        template.put("conditionalFormats", new JSONArray());
        template.put("parameters", new JSONArray());

        JSONArray datasets = new JSONArray();
        JSONObject ds = new JSONObject();
        ds.put("id", dataSet.getId());
        ds.put("name", dataSet.getSetName());
        ds.put("code", dataSet.getSetCode());
        ds.put("datasourceId", dataSet.getDsId());
        ds.put("sql", dataSet.getSqlText());
        datasets.add(ds);
        template.put("datasets", datasets);

        return template.toJSONString();
    }

    private JSONObject createTitleCell(String title) {
        JSONObject cell = new JSONObject();
        cell.put("row", 0);
        cell.put("col", 0);
        cell.put("value", title);

        JSONObject v = new JSONObject();
        v.put("v", title);
        v.put("m", title);
        v.put("fs", 18);
        v.put("bl", true);
        v.put("ht", 1);

        cell.put("value", title);
        cell.put("style", buildStyle(v));
        return cell;
    }

    private JSONObject createDescriptionCell(int row, String description) {
        JSONObject cell = new JSONObject();
        cell.put("row", row);
        cell.put("col", 0);
        cell.put("value", description);
        return cell;
    }

    private JSONObject createHeaderCell(int row, int col, String label) {
        JSONObject cell = new JSONObject();
        cell.put("row", row);
        cell.put("col", col);
        cell.put("value", label);

        JSONObject style = new JSONObject();
        style.put("fontWeight", "bold");
        style.put("backgroundColor", "#f0f5ff");
        style.put("textAlign", "center");
        style.put("fontSize", 12);
        cell.put("style", style);

        return cell;
    }

    private JSONObject createDataCell(int row, int col, String fieldName, DataSet dataSet) {
        JSONObject cell = new JSONObject();
        cell.put("row", row);
        cell.put("col", col);
        cell.put("value", "{{" + fieldName + "}}");

        JSONObject custom = new JSONObject();
        JSONObject dataBinding = new JSONObject();
        dataBinding.put("dataSetId", dataSet.getId());
        dataBinding.put("fieldName", fieldName);
        dataBinding.put("bindingType", "value");
        custom.put("dataBinding", dataBinding);
        cell.put("custom", custom);

        return cell;
    }

    private Map<String, Object> buildStyle(JSONObject v) {
        Map<String, Object> style = new HashMap<>();
        if (v.containsKey("fs")) style.put("fontSize", v.getInteger("fs"));
        if (v.containsKey("bl")) style.put("fontWeight", "bold");
        if (v.containsKey("it")) style.put("fontStyle", "italic");
        if (v.containsKey("ht")) {
            String[] aligns = {"left", "center", "right"};
            style.put("textAlign", aligns[v.getInteger("ht")]);
        }
        if (v.containsKey("fc")) style.put("color", v.getString("fc"));
        if (v.containsKey("bg")) style.put("backgroundColor", v.getString("bg"));
        return style;
    }

    private String buildDataSetBind(DataSet dataSet) {
        JSONArray bindings = new JSONArray();
        JSONObject binding = new JSONObject();
        binding.put("dataSetId", dataSet.getId());
        binding.put("bindName", "dataSet" + dataSet.getId());
        bindings.add(binding);
        return bindings.toJSONString();
    }
}
