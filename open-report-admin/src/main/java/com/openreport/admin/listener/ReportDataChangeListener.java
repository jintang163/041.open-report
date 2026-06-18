package com.openreport.admin.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openreport.admin.websocket.WebSocketPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportDataChangeListener {

    @Autowired
    private WebSocketPushService pushService;

    @KafkaListener(topics = "report-data-changed-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        log.info("收到报表数据变更消息：{}", message);
        try {
            JSONObject messageObj = JSON.parseObject(message);
            Long reportId = messageObj.getLong("reportId");
            Long scheduleId = messageObj.getLong("scheduleId");
            String status = messageObj.getString("status");
            Object result = messageObj.get("outputFilePath");

            if ("SUCCESS".equals(status) && reportId != null) {
                pushService.pushScheduleTaskFinished(scheduleId, reportId, result);
                pushService.pushDataChange(reportId);
                log.info("已推送定时任务完成和数据变更消息，reportId: {}, scheduleId: {}", reportId, scheduleId);
            }
        } catch (Exception e) {
            log.error("处理报表数据变更消息失败", e);
        }
    }
}
