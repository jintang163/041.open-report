package com.openreport.admin.controller;

import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.ReportCacheStats;
import com.openreport.admin.entity.ReportCacheWarmupConfig;
import com.openreport.admin.service.ReportAccessLogService;
import com.openreport.admin.service.ReportCacheService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "智能缓存预热管理")
@RestController
@RequestMapping("/report-cache")
public class ReportCacheController {

    @Autowired
    private ReportCacheService reportCacheService;

    @Autowired
    private ReportAccessLogService reportAccessLogService;

    @ApiOperation("查询整体缓存情况")
    @GetMapping("/info")
    @RequirePerms("report:cache:view")
    public Result<Map<String, Object>> getOverallInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("cache", reportCacheService.getOverallCacheInfo());
        info.put("config", reportAccessLogService.getOrCreateDefaultConfig());
        return Result.success(info);
    }

    @ApiOperation("查询指定报表缓存情况")
    @GetMapping("/info/{templateId}")
    @RequirePerms("report:cache:view")
    public Result<Map<String, Object>> getTemplateInfo(@PathVariable Long templateId) {
        return Result.success(reportCacheService.getCacheInfo(templateId));
    }

    @ApiOperation("手动预热单个报表")
    @PostMapping("/warmup/{templateId}")
    @RequirePerms("report:cache:warmup")
    public Result<Map<String, Object>> warmupReport(
            @PathVariable Long templateId,
            @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(reportCacheService.warmupReport(templateId, params));
    }

    @ApiOperation("批量预热高频报表")
    @PostMapping("/warmup/hot")
    @RequirePerms("report:cache:warmup")
    public Result<List<Map<String, Object>>> warmupHotReports(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer minAccessCount,
            @RequestParam(required = false) Integer statsDays) {
        return Result.success(reportCacheService.warmupHotReports(limit, minAccessCount, statsDays));
    }

    @ApiOperation("清除指定报表缓存")
    @DeleteMapping("/evict/{templateId}")
    @RequirePerms("report:cache:evict")
    public Result<Void> evictTemplateCache(@PathVariable Long templateId) {
        reportCacheService.evictCache(templateId);
        return Result.success();
    }

    @ApiOperation("清除所有报表缓存")
    @DeleteMapping("/evict/all")
    @RequirePerms("report:cache:evict")
    public Result<Void> evictAllCache() {
        reportCacheService.evictAllCache();
        return Result.success();
    }

    @ApiOperation("获取热报表排行榜")
    @GetMapping("/hot-reports")
    @RequirePerms("report:cache:view")
    public Result<List<Map<String, Object>>> getHotReports(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "20") Integer limit) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(days - 1, 0));
        return Result.success(reportAccessLogService.getTopHotReports(start, end, limit));
    }

    @ApiOperation("获取整体访问和缓存统计")
    @GetMapping("/stats/overall")
    @RequirePerms("report:cache:view")
    public Result<Map<String, Object>> getOverallStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportAccessLogService.getOverallStats(startDate, endDate));
    }

    @ApiOperation("按报表获取某日统计")
    @GetMapping("/stats/daily")
    @RequirePerms("report:cache:view")
    public Result<List<Map<String, Object>>> getDailyStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(reportAccessLogService.getDailyStatsByTemplate(date));
    }

    @ApiOperation("获取统计聚合（按日期范围）")
    @GetMapping("/stats/aggregated")
    @RequirePerms("report:cache:view")
    public Result<List<ReportCacheStats>> getAggregatedStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportAccessLogService.getStatsByDateRange(startDate, endDate));
    }

    @ApiOperation("手动触发某日统计聚合")
    @PostMapping("/stats/aggregate")
    @RequirePerms("report:cache:warmup")
    public Result<Void> aggregateStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        reportAccessLogService.aggregateDailyStats(date);
        return Result.success();
    }

    @ApiOperation("获取预热配置")
    @GetMapping("/config")
    @RequirePerms("report:cache:view")
    public Result<ReportCacheWarmupConfig> getConfig() {
        return Result.success(reportAccessLogService.getOrCreateDefaultConfig());
    }

    @ApiOperation("更新预热配置")
    @PutMapping("/config")
    @RequirePerms("report:cache:config")
    public Result<ReportCacheWarmupConfig> updateConfig(@RequestBody ReportCacheWarmupConfig config) {
        return Result.success(reportAccessLogService.updateConfig(config));
    }
}
