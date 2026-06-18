package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Api(tags = "报表执行与预览")
@RestController
@RequestMapping({"/report-execute", "/report"})
public class ReportExecuteController {

    private static final long BIG_DATA_THRESHOLD = 100_000L;
    private static final int BIG_DATA_FIRST_PAGE_SIZE = 200;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @ApiOperation("执行报表（主入口，自动探测大数据量）")
    @PostMapping("/execute/{templateId}")
    @RequirePerms("report:manage:list")
    public Result<Map<String, Object>> executeReport(
            @PathVariable Long templateId,
            @RequestBody(required = false) Map<String, Object> params) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND, "报表模板不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("templateJson", template.getTemplateJson());

        List<Map<String, Object>> tables = new ArrayList<>();
        Map<String, Object> dataSetData = new LinkedHashMap<>();
        boolean pageMode = false;
        long maxTotal = 0;

        Map<String, Object> pageInfo = new LinkedHashMap<>();

        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});

                int bindIdx = 0;
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null
                            ? binding.get("bindName").toString()
                            : "dataSet" + dataSetId;

                    long total = dataSetService.countData(dataSetId, params);
                    if (total > maxTotal) maxTotal = total;

                    Map<String, Object> tableItem = new LinkedHashMap<>();
                    List<Map<String, Object>> tableColumns = new ArrayList<>();
                    List<Map<String, Object>> tableRows = new ArrayList<>();

                    if (total >= 0 && total > BIG_DATA_THRESHOLD) {
                        pageMode = true;
                        Map<String, Object> pageResult = dataSetService.pagePreviewData(
                                dataSetId, params, 1, BIG_DATA_FIRST_PAGE_SIZE);
                        if (pageResult != null && Boolean.TRUE.equals(pageResult.get("success"))) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> cols = (List<Map<String, Object>>) pageResult.get("columns");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> rows = (List<Map<String, Object>>) pageResult.get("rows");
                            if (cols != null) {
                                for (Map<String, Object> c : cols) {
                                    Map<String, Object> tc = new LinkedHashMap<>();
                                    tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                                    tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                                    tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                                    if (c.get("width") != null) tc.put("width", c.get("width"));
                                    if (c.get("align") != null) tc.put("align", c.get("align"));
                                    tableColumns.add(tc);
                                }
                            }
                            if (rows != null) tableRows.addAll(rows);

                            Boolean hasMore = (Boolean) pageResult.get("hasMore");
                            if (bindIdx == 0) {
                                pageInfo.put("columns", cols);
                                pageInfo.put("rows", rows);
                                pageInfo.put("total", pageResult.get("total") != null ? pageResult.get("total") : total);
                                pageInfo.put("pageNum", 1);
                                pageInfo.put("pageSize", BIG_DATA_FIRST_PAGE_SIZE);
                                pageInfo.put("hasMore", hasMore != null ? hasMore : (BIG_DATA_FIRST_PAGE_SIZE < total));
                                pageInfo.put("dataSetId", dataSetId);
                                pageInfo.put("bindName", bindName);
                            }
                        }
                    } else {
                        Map<String, Object> previewResult = dataSetService.previewDataWithCount(
                                dataSetId, params, null);
                        dataSetData.put(bindName, previewResult);
                        if (previewResult != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> cols = (List<Map<String, Object>>) previewResult.get("columns");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> rows = (List<Map<String, Object>>) previewResult.get("rows");
                            if (cols != null) {
                                for (Map<String, Object> c : cols) {
                                    Map<String, Object> tc = new LinkedHashMap<>();
                                    tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                                    tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                                    tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                                    if (c.get("width") != null) tc.put("width", c.get("width"));
                                    if (c.get("align") != null) tc.put("align", c.get("align"));
                                    tableColumns.add(tc);
                                }
                            }
                            if (rows != null) tableRows.addAll(rows);
                            if (bindIdx == 0 && !pageMode) {
                                pageInfo.put("total", previewResult.get("total") != null ? previewResult.get("total") : (rows != null ? rows.size() : 0));
                            }
                        }
                    }

                    tableItem.put("bindName", bindName);
                    tableItem.put("dataSetId", dataSetId);
                    tableItem.put("columns", tableColumns);
                    tableItem.put("rows", tableRows);
                    tableItem.put("total", total);
                    tables.add(tableItem);

                    bindIdx++;
                }
            } catch (Exception e) {
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
            }
        }

        result.put("tables", tables);
        result.put("dataSets", dataSetData);

        if (pageMode) {
            result.put("pageMode", true);
            result.put("bigDataThreshold", BIG_DATA_THRESHOLD);
            result.put("page", pageInfo);
            Map<String, Object> tableData = new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pCols = (List<Map<String, Object>>) pageInfo.get("columns");
            List<Map<String, Object>> tableDataColumns = new ArrayList<>();
            if (pCols != null) {
                for (Map<String, Object> c : pCols) {
                    Map<String, Object> tc = new LinkedHashMap<>();
                    tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                    tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                    tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                    tableDataColumns.add(tc);
                }
            }
            tableData.put("columns", tableDataColumns);
            tableData.put("dataSource", pageInfo.get("rows") != null ? pageInfo.get("rows") : new ArrayList<>());
            tableData.put("total", pageInfo.get("total"));
            result.put("table", tableData);
        } else {
            result.put("pageMode", false);
            if (!tables.isEmpty()) {
                Map<String, Object> first = tables.get(0);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tCols = (List<Map<String, Object>>) first.get("columns");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tRows = (List<Map<String, Object>>) first.get("rows");
                Map<String, Object> tableData = new LinkedHashMap<>();
                tableData.put("columns", tCols);
                tableData.put("dataSource", tRows);
                tableData.put("total", first.get("total"));
                result.put("table", tableData);
            }
        }

        return Result.success(result);
    }

    @ApiOperation("获取报表参数配置")
    @GetMapping("/params/{templateId}")
    @RequirePerms("report:manage:list")
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
    @RequirePerms("report:manage:list")
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
    @RequirePerms("report:manage:export")
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
    @RequirePerms("report:manage:list")
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

    @ApiOperation("分页查询数据集预览数据（大数据量用）")
    @PostMapping("/dataset-page/{dataSetId}")
    @RequirePerms("report:manage:list")
    public Result<Map<String, Object>> pageDataSet(
            @PathVariable Long dataSetId,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        DataSet dataSet = dataSetService.getById(dataSetId);
        if (dataSet == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND);
        }
        Map<String, Object> result = dataSetService.pagePreviewData(dataSetId, params, pageNum, pageSize);
        return Result.success(result);
    }

    @ApiOperation("分页查询报表数据（大数据量滚动加载用）")
    @PostMapping("/report-data-page/{templateId}")
    @RequirePerms("report:manage:list")
    public Result<Map<String, Object>> pageReportData(
            @PathVariable Long templateId,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize,
            @RequestParam(required = false) String dataSetId) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND, "报表模板不存在");
        }
        if (template.getDataSetBind() == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("columns", new ArrayList<>());
            empty.put("rows", new ArrayList<>());
            empty.put("total", 0);
            empty.put("pageNum", pageNum);
            empty.put("pageSize", pageSize);
            empty.put("hasMore", false);
            return Result.success(empty);
        }
        try {
            List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                    new TypeReference<List<Map<String, Object>>>() {});
            if (bindings.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("columns", new ArrayList<>());
                empty.put("rows", new ArrayList<>());
                empty.put("total", 0);
                empty.put("hasMore", false);
                return Result.success(empty);
            }

            Map<String, Object> targetBind = null;
            if (dataSetId != null && !dataSetId.isEmpty()) {
                for (Map<String, Object> bind : bindings) {
                    if (dataSetId.equals(bind.get("bindName")) || dataSetId.equals(String.valueOf(bind.get("dataSetId")))) {
                        targetBind = bind;
                        break;
                    }
                }
            }
            if (targetBind == null) {
                targetBind = bindings.get(0);
            }

            Long dsId = Long.valueOf(targetBind.get("dataSetId").toString());
            String bindName = targetBind.get("bindName") != null ? targetBind.get("bindName").toString() : "dataSet" + dsId;

            Map<String, Object> pageResult = dataSetService.pagePreviewData(dsId, params, pageNum, pageSize);
            pageResult.put("bindName", bindName);
            pageResult.put("dataSetId", dsId);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.failure(ResultCode.INTERNAL_SERVER_ERROR, "分页查询失败: " + e.getMessage());
        }
    }
}
