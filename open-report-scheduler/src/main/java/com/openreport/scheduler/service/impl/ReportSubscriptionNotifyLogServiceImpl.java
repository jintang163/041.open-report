package com.openreport.scheduler.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.scheduler.entity.ReportSubscriptionNotifyLog;
import com.openreport.scheduler.enums.NotifyStatusEnum;
import com.openreport.scheduler.mapper.ReportSubscriptionNotifyLogMapper;
import com.openreport.scheduler.service.ReportSubscriptionNotifyLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReportSubscriptionNotifyLogServiceImpl extends ServiceImpl<ReportSubscriptionNotifyLogMapper, ReportSubscriptionNotifyLog> implements ReportSubscriptionNotifyLogService {

    @Override
    public ReportSubscriptionNotifyLog createLog(Long subscriptionId, Long reportId, String channel, String messageFormat, String requestData) {
        ReportSubscriptionNotifyLog log = new ReportSubscriptionNotifyLog();
        log.setSubscriptionId(subscriptionId);
        log.setReportId(reportId);
        log.setChannel(channel);
        log.setMessageFormat(messageFormat);
        log.setRequestData(requestData);
        log.setStatus(NotifyStatusEnum.PENDING.getCode());
        log.setRetryCount(0);
        log.setCostTime(0L);
        log.setCreateTime(LocalDateTime.now());
        log.setDeleted(0);
        save(log);
        return log;
    }

    @Override
    public void updateLogSuccess(Long logId, Long costTime, String responseData) {
        ReportSubscriptionNotifyLog log = getById(logId);
        if (log == null) {
            return;
        }
        log.setStatus(NotifyStatusEnum.SUCCESS.getCode());
        log.setCostTime(costTime);
        log.setResponseData(responseData);
        updateById(log);
    }

    @Override
    public void updateLogFail(Long logId, Long costTime, String errorMsg) {
        ReportSubscriptionNotifyLog log = getById(logId);
        if (log == null) {
            return;
        }
        log.setStatus(NotifyStatusEnum.FAIL.getCode());
        log.setCostTime(costTime);
        log.setErrorMsg(errorMsg);
        updateById(log);
    }
}
