package com.openreport.scheduler.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.openreport.engine.service.ReportEngineService;
import com.openreport.scheduler.entity.ReportSubscription;
import com.openreport.scheduler.entity.ReportTemplateInfo;
import com.openreport.scheduler.mapper.ReportTemplateInfoMapper;
import com.openreport.scheduler.service.NotifyDispatcher;
import com.openreport.scheduler.service.ReportSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SubscriptionPushListener {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${report.email.senderName:Open Report}")
    private String senderName;

    @Autowired
    private ReportSubscriptionService reportSubscriptionService;

    @Autowired
    private ReportTemplateInfoMapper reportTemplateInfoMapper;

    @Autowired
    private ReportEngineService reportEngineService;

    @Autowired
    private NotifyDispatcher notifyDispatcher;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "report-subscription-push-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        log.info("收到订阅推送消息：{}", message);
        JSONObject msgObj = null;
        Long subscriptionId = null;
        Long reportId = null;

        try {
            msgObj = JSON.parseObject(message);
            subscriptionId = msgObj.getLong("subscriptionId");
            reportId = msgObj.getLong("reportId");
            String channels = msgObj.getString("channels");
            String messageFormat = msgObj.getString("messageFormat");
            String contentTemplate = msgObj.getString("contentTemplate");
            String pushType = msgObj.getString("pushType");
            Integer retryCount = msgObj.getInteger("retryCount");
            if (retryCount == null) {
                retryCount = 0;
            }

            ReportTemplateInfo template = reportTemplateInfoMapper.selectById(reportId);
            if (template == null) {
                log.error("报表模板不存在，reportId: {}", reportId);
                return;
            }

            String params = msgObj.getString("params");
            Map<String, Object> paramMap = parseParams(params);

            String title = buildTitle(template, contentTemplate);
            String content = buildContent(template, messageFormat, contentTemplate, paramMap);

            Boolean includeChart = msgObj.getBoolean("includeChart");
            Boolean includeAttachment = msgObj.getBoolean("includeAttachment");
            if (Boolean.TRUE.equals(includeChart)) {
                content = appendChartInfo(content, template, paramMap);
            }

            List<String> channelList = parseChannels(channels);
            boolean allSuccess = true;

            for (String channel : channelList) {
                boolean success = dispatchToChannel(channel, msgObj, title, content, subscriptionId, reportId);
                if (!success) {
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                resetSubscriptionRetryCount(subscriptionId);
            } else if (!"MANUAL".equals(pushType)) {
                scheduleSubscriptionRetry(subscriptionId, msgObj);
            }

            log.info("订阅推送完成，subscriptionId: {}, channels: {}, success: {}", subscriptionId, channels, allSuccess);
        } catch (Exception e) {
            log.error("订阅推送异常，subscriptionId: {}", subscriptionId, e);
            if (subscriptionId != null && msgObj != null) {
                scheduleSubscriptionRetry(subscriptionId, msgObj);
            }
        }
    }

    private boolean dispatchToChannel(String channel, JSONObject msgObj, String title, String content,
                                       Long subscriptionId, Long reportId) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("singleTitle", "查看报表");
        extra.put("singleURL", "");

        switch (channel) {
            case "DINGTALK":
                String dingtalkWebhook = msgObj.getString("dingtalkWebhook");
                String dingtalkSecret = msgObj.getString("dingtalkSecret");
                if (StrUtil.isBlank(dingtalkWebhook)) {
                    log.warn("钉钉webhook为空，跳过推送，subscriptionId: {}", subscriptionId);
                    return false;
                }
                return notifyDispatcher.dispatchToChannel("DINGTALK", dingtalkWebhook, dingtalkSecret,
                        msgObj.getString("messageFormat"), title, content, extra, subscriptionId, reportId);

            case "WECOM":
                String wecomWebhook = msgObj.getString("wecomWebhook");
                if (StrUtil.isBlank(wecomWebhook)) {
                    log.warn("企微webhook为空，跳过推送，subscriptionId: {}", subscriptionId);
                    return false;
                }
                return notifyDispatcher.dispatchToChannel("WECOM", wecomWebhook, null,
                        msgObj.getString("messageFormat"), title, content, extra, subscriptionId, reportId);

            case "EMAIL":
                String emailList = msgObj.getString("emailList");
                if (StrUtil.isBlank(emailList)) {
                    log.warn("邮件收件人为空，跳过推送，subscriptionId: {}", subscriptionId);
                    return false;
                }
                String emailSubject = msgObj.getString("emailSubject");
                if (StrUtil.isBlank(emailSubject)) {
                    emailSubject = title;
                }
                return notifyDispatcher.dispatchToChannel("EMAIL", emailList, null,
                        "TEXT", emailSubject, content, extra, subscriptionId, reportId);

            default:
                log.warn("不支持的通知渠道: {}", channel);
                return false;
        }
    }

    private String buildTitle(ReportTemplateInfo template, String contentTemplate) {
        String date = LocalDateTime.now().format(DF);
        return "【" + senderName + "】" + template.getName() + " - " + date;
    }

    private String buildContent(ReportTemplateInfo template, String messageFormat, String contentTemplate,
                                Map<String, Object> paramMap) {
        if (StrUtil.isNotBlank(contentTemplate)) {
            return renderTemplate(contentTemplate, template, paramMap);
        }

        String date = LocalDateTime.now().format(DTF);
        boolean isMarkdown = "MARKDOWN".equals(messageFormat) || "CARD".equals(messageFormat);

        StringBuilder sb = new StringBuilder();
        if (isMarkdown) {
            sb.append("### ").append(template.getName()).append("\n\n");
            sb.append("---\n\n");
            sb.append("**推送时间：** ").append(date).append("\n\n");
            if (!paramMap.isEmpty()) {
                sb.append("**查询参数：**\n\n");
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    sb.append("- ").append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
                }
                sb.append("\n");
            }
            sb.append("---\n\n");
            sb.append("> 此消息由 ").append(senderName).append(" 自动推送");
        } else {
            sb.append(template.getName()).append("\n\n");
            sb.append("推送时间：").append(date).append("\n");
            if (!paramMap.isEmpty()) {
                sb.append("查询参数：\n");
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    sb.append("  ").append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
                }
            }
            sb.append("\n此消息由 ").append(senderName).append(" 自动推送");
        }

        return sb.toString();
    }

    private String renderTemplate(String template, ReportTemplateInfo reportTemplate, Map<String, Object> paramMap) {
        String result = template;
        result = result.replace("${reportName}", reportTemplate.getName());
        result = result.replace("${date}", LocalDateTime.now().format(DF));
        result = result.replace("${dateTime}", LocalDateTime.now().format(DTF));
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    private String appendChartInfo(String content, ReportTemplateInfo template, Map<String, Object> paramMap) {
        try {
            StringBuilder sb = new StringBuilder(content);
            sb.append("\n\n📊 **数据摘要：**\n\n");
            sb.append("> 图表数据将在后续版本支持内联渲染\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("附加图表信息失败", e);
            return content;
        }
    }

    private List<String> parseChannels(String channels) {
        if (StrUtil.isBlank(channels)) {
            return new ArrayList<>();
        }
        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    private Map<String, Object> parseParams(String params) {
        if (StrUtil.isBlank(params)) {
            return new HashMap<>();
        }
        try {
            return JSON.parseObject(params, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析参数失败，params: {}", params, e);
            return new HashMap<>();
        }
    }

    private void resetSubscriptionRetryCount(Long subscriptionId) {
        try {
            ReportSubscription sub = reportSubscriptionService.getById(subscriptionId);
            if (sub != null) {
                sub.setRetryCount(0);
                reportSubscriptionService.updateById(sub);
            }
        } catch (Exception e) {
            log.warn("重置订阅重试次数失败，subscriptionId: {}", subscriptionId, e);
        }
    }

    private void scheduleSubscriptionRetry(Long subscriptionId, JSONObject msgObj) {
        try {
            ReportSubscription sub = reportSubscriptionService.getById(subscriptionId);
            if (sub == null) {
                return;
            }

            int maxRetry = sub.getMaxRetryCount() != null ? sub.getMaxRetryCount() : 3;
            int currentRetry = sub.getRetryCount() != null ? sub.getRetryCount() : 0;

            if (currentRetry >= maxRetry) {
                log.warn("订阅推送已达到最大重试次数，subscriptionId: {}, retryCount: {}", subscriptionId, currentRetry);
                return;
            }

            int nextRetry = currentRetry + 1;
            sub.setRetryCount(nextRetry);
            reportSubscriptionService.updateById(sub);

            long delayMs = computeRetryDelay(nextRetry);
            msgObj.put("retryCount", nextRetry);
            msgObj.put("pushType", "RETRY");

            log.info("调度订阅推送重试，subscriptionId: {}, 第{}次重试，延迟{}ms", subscriptionId, nextRetry, delayMs);

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    KafkaTemplate<String, String> kt = ApplicationContextProvider.getBean(KafkaTemplate.class);
                    if (kt != null) {
                        kt.send("report-subscription-push-topic", msgObj.toJSONString());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } catch (Exception e) {
            log.error("调度订阅推送重试失败，subscriptionId: {}", subscriptionId, e);
        }
    }

    private long computeRetryDelay(int retryCount) {
        int baseSeconds = 10;
        int delaySeconds = baseSeconds * (int) Math.pow(2, retryCount - 1);
        int maxDelay = 3600;
        return Math.min(delaySeconds, maxDelay) * 1000L;
    }
}
