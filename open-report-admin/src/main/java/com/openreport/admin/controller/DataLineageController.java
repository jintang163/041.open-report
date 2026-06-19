package com.openreport.admin.controller;

import com.openreport.admin.entity.DataLineage;
import com.openreport.admin.service.DataLineageService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "血缘分析与影响分析")
@RestController
@RequestMapping("/lineage")
public class DataLineageController {

    @Autowired
    private DataLineageService dataLineageService;

    @ApiOperation("获取报表的血缘关系")
    @GetMapping("/report/{reportId}")
    public Result<List<DataLineage>> getLineageByReport(@PathVariable Long reportId) {
        return Result.success(dataLineageService.getLineageByReport(reportId));
    }

    @ApiOperation("获取报表指定字段的血缘追溯")
    @GetMapping("/report/{reportId}/field/{reportField}")
    public Result<Map<String, Object>> getLineageTrace(
            @PathVariable Long reportId,
            @PathVariable String reportField) {
        return Result.success(dataLineageService.getLineageTrace(reportId, reportField));
    }

    @ApiOperation("获取报表的血缘树（树形结构）")
    @GetMapping("/report/{reportId}/tree")
    public Result<Map<String, Object>> getLineageTree(@PathVariable Long reportId) {
        return Result.success(dataLineageService.getLineageTree(reportId));
    }

    @ApiOperation("获取数据集的血缘关系")
    @GetMapping("/dataset/{dataSetId}")
    public Result<List<DataLineage>> getLineageByDataSet(@PathVariable Long dataSetId) {
        return Result.success(dataLineageService.getLineageByDataSet(dataSetId));
    }

    @ApiOperation("获取数据源的血缘关系")
    @GetMapping("/datasource/{datasourceId}")
    public Result<List<DataLineage>> getLineageByDatasource(@PathVariable Long datasourceId) {
        return Result.success(dataLineageService.getLineageByDatasource(datasourceId));
    }

    @ApiOperation("获取指定表的血缘关系")
    @GetMapping("/datasource/{datasourceId}/table/{tableName}")
    public Result<List<DataLineage>> getLineageByTable(
            @PathVariable Long datasourceId,
            @PathVariable String tableName) {
        return Result.success(dataLineageService.getLineageByTable(datasourceId, tableName));
    }

    @ApiOperation("获取指定表字段的血缘关系")
    @GetMapping("/datasource/{datasourceId}/table/{tableName}/column/{columnName}")
    public Result<List<DataLineage>> getLineageByTableColumn(
            @PathVariable Long datasourceId,
            @PathVariable String tableName,
            @PathVariable String columnName) {
        return Result.success(dataLineageService.getLineageByTableColumn(datasourceId, tableName, columnName));
    }

    @ApiOperation("分析表/字段变更的影响范围")
    @GetMapping("/impact")
    public Result<Map<String, Object>> analyzeImpact(
            @RequestParam Long datasourceId,
            @RequestParam String tableName,
            @RequestParam(required = false) String columnName) {
        return Result.success(dataLineageService.analyzeImpact(datasourceId, tableName, columnName));
    }

    @ApiOperation("获取受影响的报表列表")
    @GetMapping("/impact/reports")
    public Result<List<DataLineage>> getAffectedReports(
            @RequestParam Long datasourceId,
            @RequestParam String tableName,
            @RequestParam(required = false) String columnName) {
        return Result.success(dataLineageService.getAffectedReports(datasourceId, tableName, columnName));
    }

    @ApiOperation("获取受影响的数据集列表")
    @GetMapping("/impact/datasets")
    public Result<List<DataLineage>> getAffectedDataSets(
            @RequestParam Long datasourceId,
            @RequestParam String tableName,
            @RequestParam(required = false) String columnName) {
        return Result.success(dataLineageService.getAffectedDataSets(datasourceId, tableName, columnName));
    }

    @ApiOperation("解析数据集SQL并提取血缘信息")
    @GetMapping("/parse-sql/{dataSetId}")
    public Result<Map<String, Object>> parseSqlAndExtractLineage(@PathVariable Long dataSetId) {
        return Result.success(dataLineageService.parseSqlAndExtractLineage(dataSetId));
    }

    @ApiOperation("刷新报表的血缘关系")
    @PostMapping("/refresh/report/{reportId}")
    public Result<Map<String, Object>> refreshLineageForReport(@PathVariable Long reportId) {
        return Result.success(dataLineageService.refreshLineageForReport(reportId));
    }

    @ApiOperation("刷新数据集的血缘关系（包括所有使用该数据集的报表）")
    @PostMapping("/refresh/dataset/{dataSetId}")
    public Result<Map<String, Object>> refreshLineageForDataSet(@PathVariable Long dataSetId) {
        return Result.success(dataLineageService.refreshLineageForDataSet(dataSetId));
    }

    @ApiOperation("删除报表的血缘关系")
    @DeleteMapping("/report/{reportId}")
    public Result<Boolean> deleteLineageByReport(@PathVariable Long reportId) {
        return Result.success(dataLineageService.deleteLineageByReport(reportId));
    }

    @ApiOperation("删除数据集的血缘关系")
    @DeleteMapping("/dataset/{dataSetId}")
    public Result<Boolean> deleteLineageByDataSet(@PathVariable Long dataSetId) {
        return Result.success(dataLineageService.deleteLineageByDataSet(dataSetId));
    }
}
