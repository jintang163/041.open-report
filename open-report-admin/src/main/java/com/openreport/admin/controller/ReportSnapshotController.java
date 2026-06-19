package com.openreport.admin.controller;

import com.openreport.admin.entity.ReportDataSnapshot;
import com.openreport.admin.entity.ReportSnapshotConfig;
import com.openreport.admin.service.ReportDataSnapshotService;
import com.openreport.admin.service.ReportSnapshotConfigService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Api(tags = "报表快照管理")
@RestController
@RequestMapping("/report-snapshot")
public class ReportSnapshotController {

    @Autowired
    private ReportSnapshotConfigService configService;

    @Autowired
    private ReportDataSnapshotService dataSnapshotService;

    @ApiOperation("创建快照配置")
    @PostMapping("/config")
    public Result<ReportSnapshotConfig> createConfig(@RequestBody ReportSnapshotConfig config) {
        return Result.success(configService.createConfig(config));
    }

    @ApiOperation("更新快照配置")
    @PutMapping("/config")
    public Result<ReportSnapshotConfig> updateConfig(@RequestBody ReportSnapshotConfig config) {
        return Result.success(configService.updateConfig(config));
    }

    @ApiOperation("删除快照配置")
    @DeleteMapping("/config/{id}")
    public Result<Boolean> deleteConfig(@PathVariable Long id) {
        return Result.success(configService.deleteConfig(id));
    }

    @ApiOperation("启停用快照配置")
    @PutMapping("/config/{id}/toggle")
    public Result<Boolean> toggleConfig(@PathVariable Long id, @RequestParam Integer enabled) {
        return Result.success(configService.toggleEnabled(id, enabled));
    }

    @ApiOperation("获取报表的快照配置")
    @GetMapping("/config/report/{reportId}")
    public Result<ReportSnapshotConfig> getConfigByReportId(@PathVariable Long reportId) {
        return Result.success(configService.getByReportId(reportId));
    }

    @ApiOperation("获取所有启用的快照配置列表")
    @GetMapping("/config/enabled")
    public Result<List<ReportSnapshotConfig>> listEnabledConfigs() {
        return Result.success(configService.listEnabledConfigs());
    }

    @ApiOperation("手动创建快照")
    @PostMapping("/create/{configId}")
    public Result<Map<String, Object>> createSnapshot(
            @PathVariable Long configId,
            @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(configService.createSnapshot(configId, params));
    }

    @ApiOperation("加载快照数据")
    @GetMapping("/data/{snapshotId}")
    public Result<Map<String, Object>> loadSnapshotData(@PathVariable Long snapshotId) {
        return Result.success(dataSnapshotService.loadSnapshotData(snapshotId));
    }

    @ApiOperation("获取报表的快照列表")
    @GetMapping("/data/list/{reportId}")
    public Result<List<ReportDataSnapshot>> listSnapshots(
            @PathVariable Long reportId,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return Result.success(dataSnapshotService.listByReportId(reportId, limit));
    }

    @ApiOperation("获取配置下的快照列表")
    @GetMapping("/data/list-by-config/{configId}")
    public Result<List<ReportDataSnapshot>> listSnapshotsByConfig(@PathVariable Long configId) {
        return Result.success(dataSnapshotService.listByConfigId(configId));
    }

    @ApiOperation("获取报表最新快照")
    @GetMapping("/data/latest/{reportId}")
    public Result<ReportDataSnapshot> getLatestSnapshot(@PathVariable Long reportId) {
        return Result.success(dataSnapshotService.getLatestByReportId(reportId));
    }

    @ApiOperation("按时间范围查询快照")
    @GetMapping("/data/list/{reportId}/range")
    public Result<List<ReportDataSnapshot>> listSnapshotsByTimeRange(
            @PathVariable Long reportId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(dataSnapshotService.listByReportIdAndTimeRange(reportId, startTime, endTime));
    }

    @ApiOperation("删除快照")
    @DeleteMapping("/data/{snapshotId}")
    public Result<Boolean> deleteSnapshot(@PathVariable Long snapshotId) {
        return Result.success(dataSnapshotService.removeById(snapshotId));
    }

    @ApiOperation("对比两个快照")
    @GetMapping("/compare")
    public Result<Map<String, Object>> compareSnapshots(
            @RequestParam Long baseSnapshotId,
            @RequestParam Long targetSnapshotId) {
        return Result.success(dataSnapshotService.compareSnapshots(baseSnapshotId, targetSnapshotId));
    }

    @ApiOperation("快照与实时数据对比")
    @PostMapping("/compare-realtime/{snapshotId}")
    public Result<Map<String, Object>> compareSnapshotWithRealtime(
            @PathVariable Long snapshotId,
            @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(dataSnapshotService.compareSnapshotWithRealtime(snapshotId, params));
    }

    @ApiOperation("清理过期快照")
    @PostMapping("/cleanup-expired")
    public Result<Boolean> cleanupExpired() {
        return Result.success(configService.cleanupExpiredSnapshots());
    }

    @ApiOperation("快照数据分页查询")
    @GetMapping("/data/page/{snapshotId}")
    public Result<Map<String, Object>> getSnapshotDataPage(
            @PathVariable Long snapshotId,
            @RequestParam(required = false) String bindName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "200") Integer pageSize) {
        return Result.success(dataSnapshotService.getSnapshotDataPage(
                snapshotId, bindName, pageNum, pageSize));
    }

    @ApiOperation("获取快照存储信息")
    @GetMapping("/data/storage-info/{snapshotId}")
    public Result<Map<String, Object>> getSnapshotStorageInfo(@PathVariable Long snapshotId) {
        return Result.success(dataSnapshotService.getSnapshotStorageInfo(snapshotId));
    }

    @ApiOperation("获取快照数据集列表")
    @GetMapping("/data/bind-names/{snapshotId}")
    public Result<List<String>> getSnapshotBindNames(@PathVariable Long snapshotId) {
        return Result.success(dataSnapshotService.getSnapshotBindNames(snapshotId));
    }
}
