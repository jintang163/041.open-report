package com.openreport.admin.service;

import com.openreport.admin.entity.ReportCacheWarmupConfig;
import com.openreport.admin.entity.ReportCacheStats;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportAccessLogService {

    void recordAccessAsync(Long templateId, String templateName, Long userId, String username,
                           String paramsHash, long responseTimeMs, boolean hitCache);

    List<Map<String, Object>> getTopHotReports(LocalDate startDate, LocalDate endDate, Integer limit);

    List<Map<String, Object>> getHotReportsWithThreshold(LocalDate startDate, LocalDate endDate, Integer threshold);

    Map<String, Object> getOverallStats(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDailyStatsByTemplate(LocalDate date);

    void aggregateDailyStats(LocalDate date);

    ReportCacheWarmupConfig getOrCreateDefaultConfig();

    ReportCacheWarmupConfig updateConfig(ReportCacheWarmupConfig config);

    List<ReportCacheStats> getStatsByDateRange(LocalDate startDate, LocalDate endDate);

    void incrementWarmupCount(Long templateId, LocalDate date);
}
