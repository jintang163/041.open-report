package com.openreport.scheduler.service;

import com.openreport.scheduler.entity.ReportSubscriptionNotifyLog;
import com.openreport.scheduler.service.impl.ReportSubscriptionNotifyLogServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotifyDispatcher {

    @Autowired
    private List<ChannelNotifyService> channelNotifyServices;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ReportSubscriptionNotifyLogServiceImpl notifyLogService;

    private final Map<String, ChannelNotifyService> channelServiceMap = new HashMap<>();

    public boolean dispatchToChannel(String channel, String webhook, String secret,
                                      String messageFormat, String title, String content,
                                      Map<String, Object> extra, Long subscriptionId, Long reportId) {
        long startTime = System.currentTimeMillis();

        if ("EMAIL".equals(channel)) {
            return dispatchEmail(webhook, messageFormat, title, content, subscriptionId, reportId, startTime);
        }

        ChannelNotifyService service = getChannelService(channel);
        if (service == null) {
            log.error("不支持的通知渠道: {}", channel);
            return false;
        }

        String requestData = buildRequestData(channel, messageFormat, title, content);
        ReportSubscriptionNotifyLog logEntry = notifyLogService.createLog(subscriptionId, reportId, channel, messageFormat, requestData);

        try {
            boolean success = service.send(webhook, secret, messageFormat, title, content, extra);
            long costTime = System.currentTimeMillis() - startTime;
            if (success) {
                notifyLogService.updateLogSuccess(logEntry.getId(), costTime, "OK");
                log.info("推送成功，channel: {}, subscriptionId: {}", channel, subscriptionId);
            } else {
                notifyLogService.updateLogFail(logEntry.getId(), costTime, "推送返回失败");
                log.warn("推送返回失败，channel: {}, subscriptionId: {}", channel, subscriptionId);
            }
            return success;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            notifyLogService.updateLogFail(logEntry.getId(), costTime, e.getMessage());
            log.error("推送异常，channel: {}, subscriptionId: {}", channel, subscriptionId, e);
            return false;
        }
    }

    private boolean dispatchEmail(String emailList, String messageFormat, String title, String content,
                                   Long subscriptionId, Long reportId, long startTime) {
        if (emailList == null || emailList.isEmpty()) {
            log.warn("邮件收件人为空，subscriptionId: {}", subscriptionId);
            return false;
        }

        String requestData = buildRequestData("EMAIL", messageFormat, title, content);
        ReportSubscriptionNotifyLog logEntry = notifyLogService.createLog(subscriptionId, reportId, "EMAIL", messageFormat, requestData);

        try {
            List<String> toList = parseEmails(emailList);
            boolean success = emailService.sendEmail(toList, null, title, content);
            long costTime = System.currentTimeMillis() - startTime;
            if (success) {
                notifyLogService.updateLogSuccess(logEntry.getId(), costTime, "OK");
            } else {
                notifyLogService.updateLogFail(logEntry.getId(), costTime, "邮件发送失败");
            }
            return success;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            notifyLogService.updateLogFail(logEntry.getId(), costTime, e.getMessage());
            log.error("邮件推送异常，subscriptionId: {}", subscriptionId, e);
            return false;
        }
    }

    private ChannelNotifyService getChannelService(String channel) {
        if (channelServiceMap.isEmpty()) {
            synchronized (channelServiceMap) {
                if (channelServiceMap.isEmpty()) {
                    for (ChannelNotifyService service : channelNotifyServices) {
                        channelServiceMap.put(service.getChannel(), service);
                    }
                }
            }
        }
        return channelServiceMap.get(channel);
    }

    private List<String> parseEmails(String emails) {
        return java.util.Arrays.stream(emails.split("[,;，；]"))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    private String buildRequestData(String channel, String messageFormat, String title, String content) {
        Map<String, String> data = new HashMap<>();
        data.put("channel", channel);
        data.put("messageFormat", messageFormat);
        data.put("title", title);
        data.put("contentLength", String.valueOf(content != null ? content.length() : 0));
        return com.alibaba.fastjson.JSON.toJSONString(data);
    }
}
