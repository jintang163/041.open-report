package com.openreport.scheduler.job;

import com.openreport.scheduler.config.SchedulerHttpClientConfig;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
public class ReportSnapshotJob {

    private static final Logger logger = LoggerFactory.getLogger(ReportSnapshotJob.class);

    private static final int DEFAULT_TIME_WINDOW_MINUTES = 5;

    @Autowired
    private SchedulerHttpClientConfig.SchedulerApiClient schedulerApiClient;

    @XxlJob("reportSnapshotGenerateJob")
    public void reportSnapshotGenerateJob() {
        String jobParam = XxlJobHelper.getJobParam();
        logger.info("快照生成定时任务开始执行，参数：{}", jobParam);
        XxlJobHelper.log("快照生成定时任务开始执行，参数: {}", jobParam);

        LocalDateTime now = LocalDateTime.now();

        try {
            List<Map<String, Object>> configs = fetchEnabledConfigs();
            if (configs == null || configs.isEmpty()) {
                XxlJobHelper.log("没有启用的快照配置");
                logger.info("没有启用的快照配置");
                return;
            }

            logger.info("共找到 {} 个启用的快照配置，开始检查周期匹配...", configs.size());
            XxlJobHelper.log("共找到 {} 个启用的快照配置", configs.size());

            int totalCount = 0;
            int matchedCount = 0;
            int successCount = 0;
            int failCount = 0;
            List<String> successLogs = new ArrayList<>();
            List<String> failLogs = new ArrayList<>();

            for (Map<String, Object> config : configs) {
                totalCount++;
                try {
                    Long configId = Long.valueOf(config.get("id").toString());
                    String reportName = config.get("reportName") != null ? config.get("reportName").toString() : "未知报表";
                    String cronExpression = config.get("cronExpression") != null
                            ? config.get("cronExpression").toString()
                            : null;

                    boolean shouldExecute;
                    String matchReason = "";

                    if (cronExpression == null || cronExpression.trim().isEmpty()) {
                        shouldExecute = true;
                        matchReason = "未配置Cron，每次执行";
                    } else {
                            shouldExecute = isCronMatch(cronExpression, now);
                            matchReason = shouldExecute ? "Cron匹配命中" : "Cron未命中";
                    }

                    logger.debug("配置[{}] {} - Cron: {}, 匹配结果: {}", configId, reportName, cronExpression, matchReason);

                    if (!shouldExecute) {
                        continue;
                    }

                    matchedCount++;
                    XxlJobHelper.log("开始生成快照: configId={}, reportName={}", configId, reportName);

                    Map<String, Object> result = generateSnapshot(configId);
                    Boolean success = (Boolean) result.get("success");

                    if (Boolean.TRUE.equals(success)) {
                        successCount++;
                        Object snapshotId = result.get("snapshotId");
                        Object rowCount = result.get("rowCount");
                        String logMsg = String.format("快照生成成功 - configId:%d, snapshotId:%s, 行数:%s",
                                configId, snapshotId, rowCount != null ? rowCount : "0");
                        successLogs.add(logMsg);
                        XxlJobHelper.log(logMsg);
                        logger.info("快照生成成功, configId: {}, snapshotId: {}, rowCount: {}",
                                configId, snapshotId, rowCount);
                    } else {
                        failCount++;
                        String errorMsg = result.get("message") != null ? result.get("message").toString() : "未知错误";
                        String logMsg = String.format("快照生成失败 - configId:" + configId + ", 原因:" + errorMsg);
                        failLogs.add(logMsg);
                        XxlJobHelper.log(logMsg);
                        logger.error("快照生成失败, configId: {}, message: {}", configId, errorMsg);
                    }
                } catch (Exception e) {
                    failCount++;
                    String logMsg = String.format("执行快照生成异常 - config:" + config.get("id") + ", error:" + e.getMessage());
                    failLogs.add(logMsg);
                    logger.error("执行快照生成异常, config: {}", config, e);
                    XxlJobHelper.log(logMsg);
                }
            }

            String msg = String.format("快照生成任务执行完成 - 总配置数:%d, 命中数:%d, 成功:%d, 失败:%d",
                    totalCount, matchedCount, successCount, failCount);

            if (!successLogs.isEmpty()) {
                XxlJobHelper.log("--- 成功列表 ---");
                for (String log : successLogs) {
                    XxlJobHelper.log(log);
                }
            }
            if (!failLogs.isEmpty()) {
                XxlJobHelper.log("--- 失败列表 ---");
                for (String log : failLogs) {
                    XxlJobHelper.log(log);
                }
            }

            if (failCount > 0) {
                XxlJobHelper.handleFail(msg);
            } else {
                XxlJobHelper.handleSuccess(msg);
            }
            logger.info(msg);
        } catch (Exception e) {
            logger.error("快照生成定时任务执行异常", e);
            XxlJobHelper.handleFail("快照生成定时任务执行异常: " + e.getMessage());
        }
    }

    @XxlJob("reportSnapshotCleanupJob")
    public void reportSnapshotCleanupJob() {
        logger.info("过期快照清理任务开始执行");
        XxlJobHelper.log("过期快照清理任务开始执行");
        try {
            Map<String, Object> result = schedulerApiClient.postForMap("/report-snapshot/cleanup-expired", null);
            Boolean success = (Boolean) result.get("success");

            if (Boolean.TRUE.equals(success)) {
                String msg = "过期快照清理任务执行完成";
                XxlJobHelper.handleSuccess(msg);
                logger.info(msg);
            } else {
                String errorMsg = result.get("message") != null ? result.get("message").toString() : "unknown error";
                XxlJobHelper.handleFail("过期快照清理任务失败: " + errorMsg);
                logger.error("过期快照清理任务失败: {}", errorMsg);
            }
        } catch (Exception e) {
            logger.error("过期快照清理任务执行异常", e);
            XxlJobHelper.handleFail("过期快照清理任务执行异常: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchEnabledConfigs() {
        try {
            Map<String, Object> response = schedulerApiClient.getForMap("/report-snapshot/config/enabled");
            if (Boolean.TRUE.equals(response.get("success"))) {
                Object data = response.get("data");
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            } else {
                logger.warn("获取快照配置列表失败: " + response.get("message"));
            }
        } catch (Exception e) {
            logger.error("获取快照配置列表失败", e);
        }
        return Collections.emptyList();
    }

    private Map<String, Object> generateSnapshot(Long configId) {
        try {
            Map<String, Object> response = schedulerApiClient.postForMap(
                    "/report-snapshot/create/" + configId, null);
            response.put("success", Boolean.TRUE.equals(response.get("success")));
            return response;
        } catch (Exception e) {
            logger.error("调用快照生成接口失败, configId: {}", configId, e);
            Map<String, Object> fail = new HashMap<>();
            fail.put("success", false);
            fail.put("message", "调用快照生成接口失败: " + e.getMessage());
            return fail;
        }
    }

    private boolean isCronMatch(String cronExpression, LocalDateTime dateTime) {
        try {
            String quartzCron = convertCron6To7(cronExpression);

            CronExpression cron = CronExpression.parse(quartzCron);
            LocalDateTime previous = cron.previous(dateTime);
            if (previous == null) {
                return false;
            }
            LocalDateTime windowStart = dateTime.minusMinutes(DEFAULT_TIME_WINDOW_MINUTES);
            return !previous.isBefore(windowStart) && !previous.isAfter(dateTime);
        } catch (Exception e) {
            logger.warn("Cron表达式解析失败: {}, error: {}", cronExpression, e.getMessage());
            return false;
        }
    }

    private String convertCron6To7(String cronExpression) {
        if (cronExpression == null) return "";
        String trimmed = cronExpression.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 7) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(parts[i]).append(" ");
            }
            return sb.toString().trim();
        }
        return trimmed;
    }
}
