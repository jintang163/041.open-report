package com.openreport.scheduler.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.scheduler.entity.ReportSubscription;
import com.openreport.scheduler.entity.ReportSubscriptionNotifyLog;
import com.openreport.scheduler.enums.NotifyStatusEnum;
import com.openreport.scheduler.mapper.ReportSubscriptionMapper;
import com.openreport.scheduler.mapper.ReportSubscriptionNotifyLogMapper;
import com.openreport.scheduler.service.NotifyDispatcher;
import com.openreport.scheduler.service.ReportSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportSubscriptionServiceImpl extends ServiceImpl<ReportSubscriptionMapper, ReportSubscription> implements ReportSubscriptionService {

    @Autowired
    private ReportSubscriptionNotifyLogMapper notifyLogMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Page<ReportSubscription> pageList(Integer pageNum, Integer pageSize, Long reportId, String channel, Integer status) {
        Page<ReportSubscription> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        if (reportId != null) {
            wrapper.eq(ReportSubscription::getReportId, reportId);
        }
        if (channel != null) {
            wrapper.like(ReportSubscription::getChannels, channel);
        }
        if (status != null) {
            wrapper.eq(ReportSubscription::getStatus, status);
        }
        wrapper.orderByDesc(ReportSubscription::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<ReportSubscription> listEnabled() {
        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSubscription::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public List<ReportSubscription> findDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int dayOfMonth = now.getDayOfMonth();

        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSubscription::getStatus, 1);
        wrapper.le(ReportSubscription::getNextPushTime, now);

        List<ReportSubscription> all = list(wrapper);
        return all.stream().filter(sub -> isDueForPush(sub, currentTime, dayOfWeek, dayOfMonth)).collect(java.util.stream.Collectors.toList());
    }

    private boolean isDueForPush(ReportSubscription sub, LocalTime currentTime, DayOfWeek dayOfWeek, int dayOfMonth) {
        LocalTime pushTime = sub.getPushTime();
        if (pushTime == null) {
            pushTime = LocalTime.of(9, 0);
        }

        boolean timeMatch = currentTime.getHour() == pushTime.getHour() && currentTime.getMinute() == pushTime.getMinute();

        switch (sub.getFrequency()) {
            case "DAILY":
                return timeMatch;
            case "WEEKLY":
                int targetDayOfWeek = sub.getPushDayOfWeek() != null ? sub.getPushDayOfWeek() : DayOfWeek.MONDAY.getValue();
                return timeMatch && dayOfWeek.getValue() == targetDayOfWeek;
            case "MONTHLY":
                int targetDayOfMonth = sub.getPushDayOfMonth() != null ? sub.getPushDayOfMonth() : 1;
                return timeMatch && dayOfMonth == targetDayOfMonth;
            default:
                return timeMatch;
        }
    }

    @Override
    public boolean enable(Long id) {
        ReportSubscription sub = getById(id);
        if (sub == null) {
            return false;
        }
        sub.setStatus(1);
        sub.setNextPushTime(calculateNextPushTime(sub));
        return updateById(sub);
    }

    @Override
    public boolean disable(Long id) {
        ReportSubscription sub = getById(id);
        if (sub == null) {
            return false;
        }
        sub.setStatus(0);
        return updateById(sub);
    }

    @Override
    public boolean manualPush(Long id) {
        ReportSubscription sub = getById(id);
        if (sub == null) {
            return false;
        }
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
        message.put("pushType", "MANUAL");
        try {
            kafkaTemplate.send("report-subscription-push-topic", JSON.toJSONString(message));
            return true;
        } catch (Exception e) {
            log.error("手动推送订阅消息发送失败，subscriptionId: {}", id, e);
            return false;
        }
    }

    @Override
    public Page<ReportSubscriptionNotifyLog> pageNotifyLog(Integer pageNum, Integer pageSize, Long subscriptionId, String channel, String status) {
        Page<ReportSubscriptionNotifyLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportSubscriptionNotifyLog> wrapper = new LambdaQueryWrapper<>();
        if (subscriptionId != null) {
            wrapper.eq(ReportSubscriptionNotifyLog::getSubscriptionId, subscriptionId);
        }
        if (channel != null) {
            wrapper.eq(ReportSubscriptionNotifyLog::getChannel, channel);
        }
        if (status != null) {
            wrapper.eq(ReportSubscriptionNotifyLog::getStatus, status);
        }
        wrapper.orderByDesc(ReportSubscriptionNotifyLog::getCreateTime);
        notifyLogMapper.selectPage(page, wrapper);
        return page;
    }

    private LocalDateTime calculateNextPushTime(ReportSubscription sub) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime pushTime = sub.getPushTime() != null ? sub.getPushTime() : LocalTime.of(9, 0);

        switch (sub.getFrequency()) {
            case "DAILY":
                LocalDateTime nextDaily = now.with(pushTime);
                if (!nextDaily.isAfter(now)) {
                    nextDaily = nextDaily.plusDays(1);
                }
                return nextDaily;
            case "WEEKLY":
                int targetDow = sub.getPushDayOfWeek() != null ? sub.getPushDayOfWeek() : 1;
                LocalDateTime nextWeek = now.with(pushTime);
                int currentDow = nextWeek.getDayOfWeek().getValue();
                int daysToAdd = targetDow - currentDow;
                if (daysToAdd <= 0) {
                    daysToAdd += 7;
                }
                return nextWeek.plusDays(daysToAdd);
            case "MONTHLY":
                int targetDom = sub.getPushDayOfMonth() != null ? sub.getPushDayOfMonth() : 1;
                LocalDateTime nextMonth = now.withDayOfMonth(Math.min(targetDom, now.getMonth().length(now.toLocalDate().isLeapYear()))).with(pushTime);
                if (!nextMonth.isAfter(now)) {
                    nextMonth = nextMonth.plusMonths(1);
                    nextMonth = nextMonth.withDayOfMonth(Math.min(targetDom, nextMonth.getMonth().length(nextMonth.toLocalDate().isLeapYear())));
                }
                return nextMonth;
            default:
                return now.plusDays(1).with(pushTime);
        }
    }
}
