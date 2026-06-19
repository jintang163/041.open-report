package com.openreport.scheduler.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class ReportSnapshotJob {

    private static final Logger logger = LoggerFactory.getLogger(ReportSnapshotJob.class);

    private static final String ADMIN_BASE_URL = "http://localhost:8080/api";

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }

    @XxlJob("reportSnapshotGenerateJob")
    public void reportSnapshotGenerateJob() {
        String jobParam = XxlJobHelper.getJobParam();
        logger.info("快照生成定时任务开始执行，参数：{}", jobParam);
        try {
            List<Map<String, Object>> configs = fetchEnabledConfigs();
            if (configs == null || configs.isEmpty()) {
                XxlJobHelper.log("没有需要执行的快照配置");
                logger.info("没有需要执行的快照配置");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            for (Map<String, Object> config : configs) {
                try {
                    Long configId = Long.valueOf(config.get("id").toString());
                    String cronExpression = config.get("cronExpression") != null
                            ? config.get("cronExpression").toString()
                            : null;

                    if (cronExpression != null && !cronExpression.isEmpty()) {
                        if (!shouldExecuteNow(cronExpression)) {
                            continue;
                        }
                    }

                    Map<String, Object> result = generateSnapshot(configId);
                    if (Boolean.TRUE.equals(result.get("success"))) {
                        successCount++;
                        logger.info("快照生成成功, configId: {}, snapshotId: {}", configId, result.get("snapshotId"));
                    } else {
                        failCount++;
                        logger.error("快照生成失败, configId: {}, message: {}", configId, result.get("message"));
                        XxlJobHelper.log("快照生成失败, configId: " + configId + ", message: " + result.get("message"));
                    }
                } catch (Exception e) {
                    failCount++;
                    logger.error("执行快照生成异常, config: {}", config, e);
                    XxlJobHelper.log("执行快照生成异常, error: " + e.getMessage());
                }
            }

            String msg = String.format("快照生成任务执行完成，成功: %d, 失败: %d", successCount, failCount);
            XxlJobHelper.handleSuccess(msg);
            logger.info(msg);
        } catch (Exception e) {
            logger.error("快照生成定时任务执行异常", e);
            XxlJobHelper.handleFail("快照生成定时任务执行异常: " + e.getMessage());
        }
    }

    @XxlJob("reportSnapshotCleanupJob")
    public void reportSnapshotCleanupJob() {
        logger.info("过期快照清理任务开始执行");
        try {
            String url = ADMIN_BASE_URL + "/report-snapshot/cleanup-expired";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(headers);

            Map<String, Object> response = getRestTemplate().postForObject(url, request, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                XxlJobHelper.handleSuccess("过期快照清理任务执行完成");
                logger.info("过期快照清理任务执行完成");
            } else {
                String errorMsg = response != null ? String.valueOf(response.get("message")) : "unknown error";
                XxlJobHelper.handleFail("过期快照清理任务失败: " + errorMsg);
                logger.error("过期快照清理任务失败: {}", errorMsg);
            }
        } catch (Exception e) {
            logger.error("过期快照清理任务执行异常", e);
            XxlJobHelper.handleFail("过期快照清理任务执行异常: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> fetchEnabledConfigs() {
        try {
            String url = ADMIN_BASE_URL + "/report-snapshot/config/enabled";
            Map<String, Object> response = getRestTemplate().getForObject(url, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Object data = response.get("data");
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            logger.error("获取快照配置列表失败", e);
        }
        return Collections.emptyList();
    }

    private Map<String, Object> generateSnapshot(Long configId) {
        try {
            String url = ADMIN_BASE_URL + "/report-snapshot/create/" + configId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(headers);
            Map<String, Object> response = getRestTemplate().postForObject(url, request, Map.class);
            if (response != null) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
                return response;
            }
        } catch (Exception e) {
            logger.error("调用快照生成接口失败, configId: {}", configId, e);
        }
        Map<String, Object> fail = new HashMap<>();
        fail.put("success", false);
        fail.put("message", "调用快照生成接口失败");
        return fail;
    }

    private boolean shouldExecuteNow(String cronExpression) {
        return true;
    }
}
