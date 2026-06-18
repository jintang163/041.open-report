package com.openreport.scheduler.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.common.result.Result;
import com.openreport.scheduler.entity.ReportSubscription;
import com.openreport.scheduler.entity.ReportSubscriptionNotifyLog;
import com.openreport.scheduler.service.ReportSubscriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Api(tags = "报表订阅管理")
@RestController
@RequestMapping("/report-subscription")
public class ReportSubscriptionController {

    @Autowired
    private ReportSubscriptionService reportSubscriptionService;

    @ApiOperation("分页查询订阅列表")
    @GetMapping("/page")
    public Result<Page<ReportSubscription>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Integer status) {
        return Result.success(reportSubscriptionService.pageList(pageNum, pageSize, reportId, channel, status));
    }

    @ApiOperation("获取订阅详情")
    @GetMapping("/{id}")
    public Result<ReportSubscription> getById(@PathVariable Long id) {
        return Result.success(reportSubscriptionService.getById(id));
    }

    @ApiOperation("新增订阅")
    @PostMapping
    public Result<Void> add(@RequestBody ReportSubscription subscription) {
        if (subscription.getStatus() == null) {
            subscription.setStatus(1);
        }
        if (subscription.getRetryCount() == null) {
            subscription.setRetryCount(0);
        }
        if (subscription.getMaxRetryCount() == null) {
            subscription.setMaxRetryCount(3);
        }
        if (subscription.getMessageFormat() == null) {
            subscription.setMessageFormat("MARKDOWN");
        }
        if (subscription.getFrequency() == null) {
            subscription.setFrequency("DAILY");
        }
        if (subscription.getPushTime() == null) {
            subscription.setPushTime(LocalTime.of(9, 0));
        }
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());
        subscription.setDeleted(0);
        reportSubscriptionService.save(subscription);
        return Result.success();
    }

    @ApiOperation("更新订阅")
    @PutMapping
    public Result<Void> update(@RequestBody ReportSubscription subscription) {
        subscription.setUpdateTime(LocalDateTime.now());
        reportSubscriptionService.updateById(subscription);
        return Result.success();
    }

    @ApiOperation("删除订阅")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportSubscriptionService.removeById(id);
        return Result.success();
    }

    @ApiOperation("手动推送订阅")
    @PostMapping("/push/{id}")
    public Result<Void> manualPush(@PathVariable Long id) {
        boolean success = reportSubscriptionService.manualPush(id);
        return success ? Result.success() : Result.failure("推送失败");
    }

    @ApiOperation("启用订阅")
    @PostMapping("/enable/{id}")
    public Result<Void> enable(@PathVariable Long id) {
        boolean success = reportSubscriptionService.enable(id);
        return success ? Result.success() : Result.failure("启用失败");
    }

    @ApiOperation("停用订阅")
    @PostMapping("/disable/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        boolean success = reportSubscriptionService.disable(id);
        return success ? Result.success() : Result.failure("停用失败");
    }

    @ApiOperation("查询推送日志")
    @GetMapping("/notify-log/page")
    public Result<Page<ReportSubscriptionNotifyLog>> notifyLogPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return Result.success(reportSubscriptionService.pageNotifyLog(pageNum, pageSize, subscriptionId, channel, status));
    }
}
