package com.openreport.admin.controller;

import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.service.PivotTableService;
import com.openreport.common.result.Result;
import com.openreport.engine.pivot.model.PivotTableConfig;
import com.openreport.engine.pivot.model.PivotTableResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 交叉报表控制器
 */
@Api(tags = "交叉报表管理")
@RestController
@RequestMapping("/pivot-table")
public class PivotTableController {

    @Autowired
    private PivotTableService pivotTableService;

    @ApiOperation("执行交叉报表查询")
    @PostMapping("/execute")
    @RequirePerms("pivot:tablaeexecuee")
    public Result<PivotTableResult> execute(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = (Map<String, Object>) request.get("config");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.get("params");

        PivotTableConfig config = convertToConfig(configMap);
        return Result.success(pivotTableService.executePivotQuery(config, params));
    }

    @ApiOperation("预览交叉报表数据")
    @PostMapping("/preview")
    @RequirePerms("pivot:designer:list")
    public Result<Map<String, Object>> preview(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = (Map<String, Object>) request.get("config");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        Integer limit = request.get("limit") != null ? ((Number) request.get("limit")).intValue() : 100;

        PivotTableConfig config = convertToConfig(configMap);
        return Result.success(pivotTableService.previewPivotData(config, params, limit));
    }

    @ApiOperation("生成分组SQL")
    @PostMapping("/generate-sql")
    @RequirePerms("pivot:designer:list")
    public Result<String> generateSql(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = (Map<String, Object>) request.get("config");

        PivotTableConfig config = convertToConfig(configMap);
        return Result.success(pivotTableService.generatePivotSql(config));
    }

    private PivotTableConfig convertToConfig(Map<String, Object> configMap) {
        com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject(configMap);
        return jsonObject.toJavaObject(PivotTableConfig.class);
    }
}
