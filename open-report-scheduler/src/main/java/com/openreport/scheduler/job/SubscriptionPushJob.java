package com.openreport.scheduler.job;

import com.alibaba.fastjson.JSON;
import com.openreport.scheduler.entity.ReportSubscription;
import com.openreport.scheduler.service.ReportSubscriptionService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SubscriptionPushJob {

    @Autowired
    private ReportSubscriptionService reportSubscriptionService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @XxlJob("subscriptionPushJob")
    public void subscriptionPushJob() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("订阅推送定时任务开始执行，参数：{}", jobParam);
        try {
            List<ReportSubscription> dueSubscriptions = reportSubscriptionService.findDueSubscriptions();
            if (dueSubscriptions == null || dueSubscriptions.isEmpty()) {
                XxlJobHelper.log("当前没有需要推送的订阅");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            for (ReportSubscription sub : dueSubscriptions) {
                try {
                    pushSubscription(sub);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("推送订阅失败，subscriptionId: {}", sub.getId(), e);
                }
            }

            String result = String.format("订阅推送完成，成功：%d，失败：%d", successCount, failCount);
            XxlJobHelper.handleSuccess(result);
        } catch (Exception e) {
            log.error("订阅推送定时任务执行异常", e);
            XxlJobHelper.handleFail("订阅推送定时任务执行异常: " + e.getMessage());
        }
    }

    private void pushSubscription(ReportSubscription sub) {
        Map<String, Object> message = new HashMap<>();
        message.put("subscriptionId", sub.getId());
        message.put("reportId", sub.getReportId());
        message.put("channels", sub.getChannels());
        message.put("messageFormat", sub.getMessageFormat());
        message.put("contentTemplate", sub.getContentTemplate());
        message.put("dingtalkWebhook", sub.getDingtalkWebhook());
        message.put("dingtalkSecret", sub.getDingtalkSecret());
        message.put("wecomWebhook", sub.getWecomWebhook());
        message.put("emailList", sub.getEmailList());
        message.put("emailCcList", sub.getEmailCcList());
        message.put("emailSubject", sub.getEmailSubject());
        message.put("includeChart", sub.getIncludeChart());
        message.put("includeAttachment", sub.getIncludeAttachment());
        message.put("attachmentType", sub.getAttachmentType());
        message.put("params", sub.getParams());
        message.put("pushType", "SCHEDULE");
        message.put("retryCount", 0);

        kafkaTemplate.send("report-subscription-push-topic", JSON.toJSONString(message));

        sub.setLastPushTime(java.time.LocalDateTime.now());
        reportSubscriptionService.updateById(sub);
        log.info("已发送订阅推送消息，subscriptionId: {}, reportId: {}", sub.getId(), sub.getReportId());
    }
}
