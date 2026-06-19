package com.openreport.admin.service.impl;

import com.openreport.admin.entity.ReportAccessLog;
import com.openreport.admin.entity.ReportCacheStats;
import com.openreport.admin.entity.ReportCacheWarmupConfig;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.mapper.ReportAccessLogMapper;
import com.openreport.admin.mapper.ReportCacheStatsMapper;
import com.openreport.admin.mapper.ReportCacheWarmupConfigMapper;
import com.openreport.admin.service.ReportAccessLogService;
import com.openreport.admin.service.ReportTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportAccessLogServiceImpl implements ReportAccessLogService {

    @Autowired
    private ReportAccessLogMapper accessLogMapper;

    @Autowired
    private ReportCacheStatsMapper cacheStatsMapper;

    @Autowired
    private ReportCacheWarmupConfigMapper configMapper;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Override
    @Async
    public void recordAccessAsync(Long templateId, String templateName, Long userId, String username,
                                  String paramsHash, long responseTimeMs, boolean hitCache) {
        try {
            LocalDateTime now = LocalDateTime.now();
            ReportAccessLog accessLog = new ReportAccessLog();
            accessLog.setTemplateId(templateId);
            accessLog.setTemplateName(templateName);
            accessLog.setUserId(userId);
            accessLog.setUsername(username);
            accessLog.setAccessDate(now.toLocalDate());
            accessLog.setAccessHour(now.getHour());
            accessLog.setParamsHash(paramsHash);
            accessLog.setResponseTimeMs(responseTimeMs);
            accessLog.setHitCache(hitCache ? 1 : 0);
            accessLog.setCreateTime(now);
            accessLogMapper.insert(accessLog);
        } catch (Exception e) {
            log.warn("记录报表访问日志失败: templateId={}", templateId, e);
        }
    }

    @Override
    public List<Map<String, Object>> getTopHotReports(LocalDate startDate, LocalDate endDate, Integer limit) {
        return accessLogMapper.selectTopHotReports(startDate, endDate, limit != null ? limit : 20);
    }

    @Override
    public List<Map<String, Object>> getHotReportsWithThreshold(LocalDate startDate, LocalDate endDate, Integer threshold) {
        return accessLogMapper.selectHotReportsWithThreshold(startDate, endDate, threshold);
    }

    @Override
    public List<Map<String, Object>> getHotParamCombos(LocalDate startDate, LocalDate endDate, Integer threshold, Integer limit) {
        return accessLogMapper.selectHotReportParamCombos(startDate, endDate, threshold,
                limit != null ? limit : 100);
    }

    @Override
    public Map<String, Object> getOverallStats(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> list = accessLogMapper.selectOverallStats(startDate, endDate);
        long totalRequests = 0;
        long totalHits = 0;
        long totalMisses = 0;
        long totalRespTime = 0;
        int days = 0;
        for (Map<String, Object> m : list) {
            Object tr = m.get("total_requests");
            Object ch = m.get("cache_hits");
            Object cm = m.get("cache_misses");
            Object art = m.get("avg_response_time_ms");
            if (tr != null) totalRequests += Long.parseLong(tr.toString());
            if (ch != null) totalHits += Long.parseLong(ch.toString());
            if (cm != null) totalMisses += Long.parseLong(cm.toString());
            if (art != null) totalRespTime += Long.parseLong(art.toString());
            days++;
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("days", days);
        result.put("totalRequests", totalRequests);
        result.put("totalCacheHits", totalHits);
        result.put("totalCacheMisses", totalMisses);
        result.put("hitRate", totalRequests > 0 ? String.format("%.2f%%", 100.0 * totalHits / totalRequests) : "0.00%");
        result.put("avgResponseTimeMs", days > 0 ? totalRespTime / days : 0);
        result.put("daily", list);
        return result;
    }

    @Override
    public List<Map<String, Object>> getDailyStatsByTemplate(LocalDate date) {
        return accessLogMapper.selectDailyStatsByTemplate(date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDailyStats(LocalDate date) {
        log.info("开始聚合 {} 的缓存统计", date);
        List<Map<String, Object>> daily = getDailyStatsByTemplate(date);
        if (daily == null || daily.isEmpty()) {
            log.info("{} 没有访问数据", date);
            return;
        }
        int inserted = 0;
        int updated = 0;
        for (Map<String, Object> d : daily) {
            try {
                Long templateId = Long.valueOf(String.valueOf(d.get("template_id")));
                String templateName = d.get("template_name") != null ? String.valueOf(d.get("template_name")) : null;
                if (templateName == null || templateName.isEmpty()) {
                    ReportTemplate tpl = reportTemplateService.getById(templateId);
                    if (tpl != null) templateName = tpl.getTemplateName();
                }
                long totalRequests = parseLong(d.get("total_requests"));
                long cacheHits = parseLong(d.get("cache_hits"));
                long cacheMisses = parseLong(d.get("cache_misses"));
                long avgResp = parseLong(d.get("avg_response_time_ms"));

                ReportCacheStats existing = cacheStatsMapper.selectByTemplateAndDate(templateId, date);
                LocalDateTime now = LocalDateTime.now();
                if (existing == null) {
                    ReportCacheStats stats = new ReportCacheStats();
                    stats.setTemplateId(templateId);
                    stats.setTemplateName(templateName);
                    stats.setStatsDate(date);
                    stats.setTotalRequests(totalRequests);
                    stats.setCacheHits(cacheHits);
                    stats.setCacheMisses(cacheMisses);
                    stats.setWarmupCount(0L);
                    stats.setAvgResponseTimeMs(avgResp);
                    stats.setCreateTime(now);
                    stats.setUpdateTime(now);
                    cacheStatsMapper.insertStats(stats);
                    inserted++;
                } else {
                    existing.setTotalRequests(totalRequests);
                    existing.setCacheHits(cacheHits);
                    existing.setCacheMisses(cacheMisses);
                    existing.setAvgResponseTimeMs(avgResp);
                    existing.setTemplateName(templateName);
                    existing.setUpdateTime(now);
                    cacheStatsMapper.updateStats(existing);
                    updated++;
                }
            } catch (Exception e) {
                log.error("聚合缓存统计失败: {}", d, e);
            }
        }
        log.info("缓存统计聚合完成: 新增={}, 更新={}", inserted, updated);
    }

    @Override
    public ReportCacheWarmupConfig getOrCreateDefaultConfig() {
        ReportCacheWarmupConfig config = configMapper.selectActiveConfig();
        if (config != null) return config;

        LocalDateTime now = LocalDateTime.now();
        config = new ReportCacheWarmupConfig();
        config.setConfigName("默认缓存预热配置");
        config.setEnabled(1);
        config.setHotThreshold(50);
        config.setStatsWindowDays(7);
        config.setMaxHotReports(50);
        config.setLowPeakStartHour(2);
        config.setLowPeakEndHour(5);
        config.setCacheTtlSeconds(43200L);
        config.setDefaultParamsJson("{}");
        config.setCreateTime(now);
        config.setUpdateTime(now);
        config.setDeleted(0);
        configMapper.insert(config);
        return config;
    }

    @Override
    public ReportCacheWarmupConfig updateConfig(ReportCacheWarmupConfig config) {
        if (config.getId() == null) {
            ReportCacheWarmupConfig existing = configMapper.selectActiveConfig();
            if (existing != null) {
                config.setId(existing.getId());
            }
        }
        config.setUpdateTime(LocalDateTime.now());
        if (config.getId() != null) {
            configMapper.updateById(config);
        } else {
            config.setCreateTime(LocalDateTime.now());
            configMapper.insert(config);
        }
        return configMapper.selectById(config.getId());
    }

    @Override
    public List<ReportCacheStats> getStatsByDateRange(LocalDate startDate, LocalDate endDate) {
        return cacheStatsMapper.selectByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementWarmupCount(Long templateId, LocalDate date) {
        ReportCacheStats stats = cacheStatsMapper.selectByTemplateAndDate(templateId, date);
        LocalDateTime now = LocalDateTime.now();
        if (stats == null) {
            ReportTemplate tpl = reportTemplateService.getById(templateId);
            stats = new ReportCacheStats();
            stats.setTemplateId(templateId);
            stats.setTemplateName(tpl != null ? tpl.getTemplateName() : null);
            stats.setStatsDate(date);
            stats.setTotalRequests(0L);
            stats.setCacheHits(0L);
            stats.setCacheMisses(0L);
            stats.setWarmupCount(1L);
            stats.setAvgResponseTimeMs(0L);
            stats.setCreateTime(now);
            stats.setUpdateTime(now);
            cacheStatsMapper.insertStats(stats);
        } else {
            stats.setWarmupCount((stats.getWarmupCount() != null ? stats.getWarmupCount() : 0L) + 1);
            stats.setUpdateTime(now);
            cacheStatsMapper.updateStats(stats);
        }
    }

    private long parseLong(Object obj) {
        if (obj == null) return 0;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
