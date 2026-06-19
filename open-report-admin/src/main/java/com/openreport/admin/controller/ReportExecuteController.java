package com.openreport.admin.controller;

import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.ReportCacheWarmupConfig;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportAccessLogService;
import com.openreport.admin.service.ReportCacheService;
import com.openreport.admin.service.ReportExecuteService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Api(tags = "报表执行与预览")
@RestController
@RequestMapping({"/report-execute", "/report"})
public class ReportExecuteController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ReportExecuteService reportExecuteService;

    @Autowired
    private ReportCacheService reportCacheService;

    @Autowired
    private ReportAccessLogService reportAccessLogService;

    @ApiOperation("执行报表（主入口，自动探测大数据量+智能缓存）")
    @PostMapping("/execute/{templateId}")
    @RequirePerms("report:manage:list")
    public Result<Map<String, Object>> executeReport(
            @PathVariable Long templateId,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestParam(required = false) String snapshotMode,
            @RequestParam(required = false) Long snapshotId,
            @RequestParam(required = false, defaultValue = "true") Boolean useCache) {
        long startTime = System.currentTimeMillis();

        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND, "报表模板不存在");
        }

        Long userId = SecurityContextHolder.getUserId();
        String username = SecurityContextHolder.getUsername();
        String paramsHash = reportCacheService.computeParamsHash(params);
        boolean hitCache = false;
        Map<String, Object> result = null;

        if (Boolean.TRUE.equals(useCache) && (snapshotMode == null || snapshotMode.isEmpty())) {
            result = reportCacheService.getCachedReport(templateId, paramsHash);
            if (result != null) {
                hitCache = true;
                log.debug("报表命中缓存: templateId={}, paramsHash={}", templateId, paramsHash);
            }
        }

        if (result == null) {
            result = reportExecuteService.executeReport(templateId, params, snapshotMode, snapshotId);
            Boolean success = (Boolean) result.get("success");
            if (success == null) success = true;
            if (Boolean.TRUE.equals(success) && (snapshotMode == null || snapshotMode.isEmpty())) {
                try {
                    ReportCacheWarmupConfig config = reportAccessLogService.getOrCreateDefaultConfig();
                    long ttl = config.getCacheTtlSeconds() != null ? config.getCacheTtlSeconds() : 43200L;
                    reportCacheService.cacheReport(templateId, paramsHash, result, ttl);
                    reportCacheService.saveParamsMapping(templateId, paramsHash, params);
                } catch (Exception e) {
                    log.warn("自动缓存报表结果失败: templateId={}", templateId, e);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        try {
            reportAccessLogService.recordAccessAsync(
                    templateId, template.getTemplateName(),
                    userId, username,
                    paramsHash, elapsed, hitCache);
        } catch (Exception e) {
            log.warn("记录访问日志失败", e);
        }

        if (hitCache) {
            result.putIfAbsent("hitCache", true);
            result.putIfAbsent("cacheParamsHash", paramsHash);
        } else {
            result.putIfAbsent("hitCache", false);
        }
        result.putIfAbsent("responseTimeMs", elapsed);
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
