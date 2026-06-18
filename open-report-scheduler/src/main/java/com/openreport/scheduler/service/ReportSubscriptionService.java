package com.openreport.scheduler.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.scheduler.entity.ReportSubscription;
import com.openreport.scheduler.entity.ReportSubscriptionNotifyLog;

import java.util.List;

public interface ReportSubscriptionService extends IService<ReportSubscription> {

    Page<ReportSubscription> pageList(Integer pageNum, Integer pageSize, Long reportId, String channel, Integer status);

    List<ReportSubscription> listEnabled();

    List<ReportSubscription> findDueSubscriptions();

    boolean enable(Long id);

    boolean disable(Long id);

    boolean manualPush(Long id);

    Page<ReportSubscriptionNotifyLog> pageNotifyLog(Integer pageNum, Integer pageSize, Long subscriptionId, String channel, String status);
}
