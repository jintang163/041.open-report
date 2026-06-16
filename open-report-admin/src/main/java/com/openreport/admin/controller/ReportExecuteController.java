package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Api(tags = "报表执行与预览")
@RestController
@RequestMapping("/report-execute")
public class ReportExecuteController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @ApiOperation("获取报表参数配置")
    @GetMapping("/params/{templateId}")
    public Result<List<Map<String, Object>>> getReportParams(@PathVariable Long templateId) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("报表模板不存在");
        }
        List<Map<String, Object>> params = new ArrayList<>();
        if (template.getParamConfig() != null) {
            try {
                params = JSON.parseObject(template.getParamConfig(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }
        return Result.success(params);
    }

    @ApiOperation("预览报表数据")
    @PostMapping("/preview/{templateId}")
    public Result<Map<String, Object>> previewReport(
            @PathVariable Long templateId,
            @RequestBody(required = false) Map<String, Object> params) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("报表模板不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("templateJson", template.getTemplateJson());
        Map<String, Object> dataSetData = new HashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, 1000);
                    dataSetData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
            }
        }
        result.put("dataSets", dataSetData);
        return Result.success(result);
    }

    @ApiOperation("导出报表数据")
    @PostMapping("/export/{templateId}")
    public Result<Map<String, Object>> exportReport(
            @PathVariable Long templateId,
            @RequestParam(defaultValue = "excel") String exportType,
            @RequestBody(required = false) Map<String, Object> params) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("报表模板不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("exportType", exportType);
        Map<String, Object> exportData = new HashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, null);
                    exportData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
            }
        }
        result.put("exportData", exportData);
        result.put("fileName", template.getTemplateName() + "_" + System.currentTimeMillis());
        return Result.success(result);
    }

    @ApiOperation("获取单个数据集预览数据（报表设计器用）")
    @PostMapping("/dataset-preview/{dataSetId}")
    public Result<Map<String, Object>> previewDataSet(
            @PathVariable Long dataSetId,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "500") Integer limit) {
        DataSet dataSet = dataSetService.getById(dataSetId);
        if (dataSet == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND);
        }
        Map<String, Object> result = dataSetService.previewData(dataSetId, params, limit);
        return Result.success(result);
    }
}
