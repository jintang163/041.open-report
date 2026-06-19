package com.openreport.scheduler.job;

import com.openreport.scheduler.config.SchedulerHttpClientConfig;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ReportCacheWarmupJob {

    private static final Logger logger = LoggerFactory.getLogger(ReportCacheWarmupJob.class);

    @Autowired
    private SchedulerHttpClientConfig.SchedulerApiClient schedulerApiClient;

    @XxlJob("reportCacheWarmupJob")
    public void reportCacheWarmupJob() {
        String jobParam = XxlJobHelper.getJobParam();
        logger.info("智能缓存预热任务开始执行，参数：{}", jobParam);
        XxlJobHelper.log("智能缓存预热任务开始执行，参数: {}", jobParam);

        try {
            Map<String, Object> config = fetchConfig();
            if (config == null || config.isEmpty()) {
                XxlJobHelper.log("未获取到预热配置，使用默认参数");
                config = defaultConfig();
            }

            if (!isEnabled(config)) {
                String msg = "缓存预热功能未启用";
                XxlJobHelper.handleSuccess(msg);
                logger.info(msg);
                return;
            }

            if (!isLowPeakWindow(config)) {
                String msg = String.format("当前时间 %s 不在低峰期窗口内，跳过执行", LocalDateTime.now());
                XxlJobHelper.handleSuccess(msg);
                logger.info(msg);
                return;
            }

            Integer limit = getIntParam(config, "maxHotReports", jobParam, "limit", 50);
            Integer threshold = getIntParam(config, "hotThreshold", jobParam, "threshold", 50);
            Integer statsDays = getIntParam(config, "statsWindowDays", jobParam, "statsDays", 7);

            logger.info("开始批量预热高频报表: limit={}, threshold={}, statsDays={}", limit, threshold, statsDays);
            XxlJobHelper.log("预热参数: limit={}, threshold={}, statsDays={}", limit, threshold, statsDays);

            Map<String, Object> warmupResult = schedulerApiClient.postForMap(
                    String.format("/report-cache/warmup/hot?limit=%d&minAccessCount=%d&statsDays=%d",
                            limit, threshold, statsDays), null);

            logger.info("预热执行结果: {}", warmupResult);
            Boolean success = (Boolean) warmupResult.get("success");

            Object data = warmupResult.get("data");
            int total = 0;
            int successCount = 0;
            int failCount = 0;
            List<String> successLogs = new ArrayList<>();
            List<String> failLogs = new ArrayList<>();

            if (data instanceof List) {
                List<?> list = (List<?>) data;
                total = list.size();
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) item;
                        Object templateId = m.get("templateId");
                        Object itemSuccess = m.get("success");
                        Object accessCount = m.get("accessCount");
                        Object elapsed = m.get("elapsedMs");
                        Object msg = m.get("message");
                        if (Boolean.TRUE.equals(itemSuccess)) {
                            successCount++;
                            successLogs.add(String.format("成功: templateId=%s, accessCount=%s, elapsed=%sms",
                                    templateId, accessCount != null ? accessCount : "-", elapsed != null ? elapsed : "-"));
                        } else {
                            failCount++;
                            failLogs.add(String.format("失败: templateId=%s, 原因=%s",
                                    templateId, msg != null ? msg : "未知"));
                        }
                    }
                }
            }

            String summary = String.format("高频报表预热完成 - 总数:%d, 成功:%d, 失败:%d", total, successCount, failCount);
            if (!successLogs.isEmpty()) {
                XxlJobHelper.log("--- 成功列表 ---");
                for (String s : successLogs) XxlJobHelper.log(s);
            }
            if (!failLogs.isEmpty()) {
                XxlJobHelper.log("--- 失败列表 ---");
                for (String f : failLogs) XxlJobHelper.log(f);
            }

            if (failCount > 0) {
                XxlJobHelper.handleFail(summary);
            } else {
                XxlJobHelper.handleSuccess(summary);
            }
            logger.info(summary);
        } catch (Exception e) {
            logger.error("智能缓存预热任务执行异常", e);
            XxlJobHelper.handleFail("智能缓存预热任务执行异常: " + e.getMessage());
        }
    }

    @XxlJob("reportCacheStatsAggregateJob")
    public void reportCacheStatsAggregateJob() {
        logger.info("缓存统计聚合任务开始执行");
        XxlJobHelper.log("缓存统计聚合任务开始执行");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String dateStr = yesterday.toString();
            Map<String, Object> result = schedulerApiClient.postForMap(
                    "/report-cache/stats/aggregate?date=" + dateStr, null);

            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                String msg = "缓存统计聚合任务执行完成 - 日期: " + dateStr;
                XxlJobHelper.handleSuccess(msg);
                logger.info(msg);
            } else {
                String errorMsg = result.get("message") != null ? result.get("message").toString() : "unknown error";
                XxlJobHelper.handleFail("缓存统计聚合任务失败: " + errorMsg);
                logger.error("缓存统计聚合任务失败: {}", errorMsg);
            }
        } catch (Exception e) {
            logger.error("缓存统计聚合任务执行异常", e);
            XxlJobHelper.handleFail("缓存统计聚合任务执行异常: " + e.getMessage());
        }
    }

    @XxlJob("reportCacheCleanupJob")
    public void reportCacheCleanupJob() {
        logger.info("过期缓存清理任务开始执行（按 TTL 自动过期，此处仅记录统计信息）");
        XxlJobHelper.log("过期缓存清理任务开始执行");
        try {
            Map<String, Object> info = schedulerApiClient.getForMap("/report-cache/info");
            if (info != null && Boolean.TRUE.equals(info.get("success"))) {
                Object data = info.get("data");
                String summary = String.format("缓存状态检查完成: %s", data != null ? data.toString() : "无数据");
                XxlJobHelper.handleSuccess(summary);
                logger.info(summary);
            } else {
                XxlJobHelper.handleSuccess("缓存状态检查完成（无数据）");
            }
        } catch (Exception e) {
            logger.error("缓存检查任务异常", e);
            XxlJobHelper.handleFail("缓存检查任务异常: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchConfig() {
        try {
            Map<String, Object> response = schedulerApiClient.getForMap("/report-cache/config");
            if (Boolean.TRUE.equals(response.get("success"))) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            }
        } catch (Exception e) {
            logger.warn("获取预热配置失败，使用默认配置", e);
        }
        return defaultConfig();
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("enabled", 1);
        cfg.put("hotThreshold", 50);
        cfg.put("statsWindowDays", 7);
        cfg.put("maxHotReports", 50);
        cfg.put("lowPeakStartHour", 2);
        cfg.put("lowPeakEndHour", 5);
        return cfg;
    }

    private boolean isEnabled(Map<String, Object> config) {
        Object enabled = config.get("enabled");
        return enabled == null || Integer.parseInt(enabled.toString()) == 1;
    }

    private boolean isLowPeakWindow(Map<String, Object> config) {
        int start = getInt(config, "lowPeakStartHour", 0);
        int end = getInt(config, "lowPeakEndHour", 24);
        int hour = LocalDateTime.now().getHour();
        if (start <= end) {
            return hour >= start && hour < end;
        } else {
            return hour >= start || hour < end;
        }
    }

    private int getIntParam(Map<String, Object> config, String cfgKey,
                            String jobParam, String paramKey, int defaultValue) {
        if (jobParam != null && !jobParam.isEmpty()) {
            try {
                for (String part : jobParam.split("[,&]")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2 && paramKey.equalsIgnoreCase(kv[0].trim())) {
                        return Integer.parseInt(kv[1].trim());
                    }
                }
            } catch (Exception ignored) {}
        }
        return getInt(config, cfgKey, defaultValue);
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object v = map.get(key);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
