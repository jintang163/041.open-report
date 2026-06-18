package com.openreport.scheduler.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.common.result.Result;
import com.openreport.scheduler.entity.ReportSchedule;
import com.openreport.scheduler.service.ReportScheduleService;
import com.openreport.scheduler.service.ScheduleJobManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Api(tags = "报表调度管理")
@RestController
@RequestMapping("/report-schedule")
public class ReportScheduleController {

    @Autowired
    private ReportScheduleService reportScheduleService;

    @Autowired
    private ScheduleJobManager scheduleJobManager;

    @ApiOperation("分页查询报表调度列表")
    @GetMapping("/page")
    public Result<Page<ReportSchedule>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Integer status) {
        return Result.success(reportScheduleService.pageList(pageNum, pageSize, reportId, status));
    }

    @ApiOperation("获取报表调度详情")
    @GetMapping("/{id}")
    public Result<ReportSchedule> getById(@PathVariable Long id) {
        return Result.success(reportScheduleService.getById(id));
    }

    @ApiOperation("新增报表调度")
    @PostMapping
    public Result<Void> add(@RequestBody ReportSchedule schedule) {
        if (schedule.getStatus() == null) {
            schedule.setStatus(1);
        }
        if (schedule.getRetryCount() == null) {
            schedule.setRetryCount(0);
        }
        if (schedule.getMaxRetryCount() == null) {
            schedule.setMaxRetryCount(3);
        }
        schedule.setCreateTime(LocalDateTime.now());
        schedule.setUpdateTime(LocalDateTime.now());
        schedule.setDeleted(0);
        reportScheduleService.save(schedule);

        if (schedule.getStatus() != null && schedule.getStatus() == 1) {
            scheduleJobManager.addJob(schedule);
        }

        return Result.success();
    }

    @ApiOperation("更新报表调度")
    @PutMapping
    public Result<Void> update(@RequestBody ReportSchedule schedule) {
        schedule.setUpdateTime(LocalDateTime.now());
        reportScheduleService.updateById(schedule);
        scheduleJobManager.updateJob(schedule);
        return Result.success();
    }

    @ApiOperation("删除报表调度")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportScheduleService.removeById(id);
        scheduleJobManager.deleteJob(id);
        return Result.success();
    }

    @ApiOperation("手动触发报表执行")
    @PostMapping("/trigger/{id}")
    public Result<Void> trigger(@PathVariable Long id) {
        boolean success = reportScheduleService.trigger(id);
        return success ? Result.success() : Result.failure("触发失败");
    }

    @ApiOperation("启用报表调度")
    @PostMapping("/enable/{id}")
    public Result<Void> enable(@PathVariable Long id) {
        ReportSchedule schedule = reportScheduleService.getById(id);
        if (schedule == null) {
            return Result.failure("任务不存在");
        }
        schedule.setStatus(1);
        schedule.setUpdateTime(LocalDateTime.now());
        reportScheduleService.updateById(schedule);

        if (scheduleJobManager.checkExists(id)) {
            scheduleJobManager.resumeJob(id);
        } else {
            scheduleJobManager.addJob(schedule);
        }
        return Result.success();
    }

    @ApiOperation("停用报表调度")
    @PostMapping("/disable/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        ReportSchedule schedule = reportScheduleService.getById(id);
        if (schedule == null) {
            return Result.failure("任务不存在");
        }
        schedule.setStatus(0);
        schedule.setUpdateTime(LocalDateTime.now());
        reportScheduleService.updateById(schedule);
        scheduleJobManager.pauseJob(id);
        return Result.success();
    }
}
