package com.openreport.scheduler.service;

import com.openreport.scheduler.entity.ReportSubscriptionNotifyLog;

public interface ReportSubscriptionNotifyLogService extends com.baomidou.mybatisplus.extension.service.IService<ReportSubscriptionNotifyLog> {

    ReportSubscriptionNotifyLog createLog(Long subscriptionId, Long reportId, String channel, String messageFormat, String requestData);

    void updateLogSuccess(Long logId, Long costTime, String responseData);

    void updateLogFail(Long logId, Long costTime, String errorMsg);
}
