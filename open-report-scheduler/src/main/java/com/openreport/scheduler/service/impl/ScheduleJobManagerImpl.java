package com.openreport.scheduler.service.impl;

import com.openreport.scheduler.entity.ReportSchedule;
import com.openreport.scheduler.job.ReportQuartzJob;
import com.openreport.scheduler.service.ReportScheduleService;
import com.openreport.scheduler.service.ScheduleJobManager;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class ScheduleJobManagerImpl implements ScheduleJobManager {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleJobManagerImpl.class);

    private static final String JOB_GROUP = "REPORT_JOB_GROUP";
    private static final String TRIGGER_GROUP = "REPORT_TRIGGER_GROUP";

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ReportScheduleService reportScheduleService;

    @PostConstruct
    public void init() {
        try {
            scheduler.start();
            logger.info("Quartz 调度器已启动");
        } catch (SchedulerException e) {
            logger.error("Quartz 调度器启动失败", e);
        }
    }

    @Override
    public void addJob(ReportSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }
        try {
            JobDetail jobDetail = buildJobDetail(schedule);
            Trigger trigger = buildTrigger(schedule);
            scheduler.scheduleJob(jobDetail, trigger);
            logger.info("添加调度任务成功，scheduleId: {}, cron: {}", schedule.getId(), schedule.getCronExpression());
        } catch (SchedulerException e) {
            logger.error("添加调度任务失败，scheduleId: {}", schedule.getId(), e);
            throw new RuntimeException("添加调度任务失败", e);
        }
    }

    @Override
    public void updateJob(ReportSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(String.valueOf(schedule.getId()), TRIGGER_GROUP);
            if (!scheduler.checkExists(triggerKey)) {
                addJob(schedule);
                return;
            }

            Trigger oldTrigger = scheduler.getTrigger(triggerKey);
            CronTrigger newTrigger = buildTrigger(schedule);
            scheduler.rescheduleJob(triggerKey, newTrigger);

            JobKey jobKey = JobKey.jobKey(String.valueOf(schedule.getId()), JOB_GROUP);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            jobDetail.getJobDataMap().put("scheduleId", schedule.getId());

            if (schedule.getStatus() != null && schedule.getStatus() == 1) {
                scheduler.resumeJob(jobKey);
            } else {
                scheduler.pauseJob(jobKey);
            }

            logger.info("更新调度任务成功，scheduleId: {}, cron: {}", schedule.getId(), schedule.getCronExpression());
        } catch (SchedulerException e) {
            logger.error("更新调度任务失败，scheduleId: {}", schedule.getId(), e);
            throw new RuntimeException("更新调度任务失败", e);
        }
    }

    @Override
    public void deleteJob(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        try {
            JobKey jobKey = JobKey.jobKey(String.valueOf(scheduleId), JOB_GROUP);
            boolean result = scheduler.deleteJob(jobKey);
            logger.info("删除调度任务，scheduleId: {}, result: {}", scheduleId, result);
        } catch (SchedulerException e) {
            logger.error("删除调度任务失败，scheduleId: {}", scheduleId, e);
            throw new RuntimeException("删除调度任务失败", e);
        }
    }

    @Override
    public void pauseJob(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        try {
            JobKey jobKey = JobKey.jobKey(String.valueOf(scheduleId), JOB_GROUP);
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
                logger.info("暂停调度任务，scheduleId: {}", scheduleId);
            }
        } catch (SchedulerException e) {
            logger.error("暂停调度任务失败，scheduleId: {}", scheduleId, e);
            throw new RuntimeException("暂停调度任务失败", e);
        }
    }

    @Override
    public void resumeJob(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        try {
            JobKey jobKey = JobKey.jobKey(String.valueOf(scheduleId), JOB_GROUP);
            if (scheduler.checkExists(jobKey)) {
                scheduler.resumeJob(jobKey);
                logger.info("恢复调度任务，scheduleId: {}", scheduleId);
            }
        } catch (SchedulerException e) {
            logger.error("恢复调度任务失败，scheduleId: {}", scheduleId, e);
            throw new RuntimeException("恢复调度任务失败", e);
        }
    }

    @Override
    public void triggerJob(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        try {
            JobKey jobKey = JobKey.jobKey(String.valueOf(scheduleId), JOB_GROUP);
            if (scheduler.checkExists(jobKey)) {
                scheduler.triggerJob(jobKey);
                logger.info("手动触发调度任务，scheduleId: {}", scheduleId);
            }
        } catch (SchedulerException e) {
            logger.error("手动触发调度任务失败，scheduleId: {}", scheduleId, e);
            throw new RuntimeException("手动触发调度任务失败", e);
        }
    }

    @Override
    public boolean checkExists(Long scheduleId) {
        if (scheduleId == null) {
            return false;
        }
        try {
            JobKey jobKey = JobKey.jobKey(String.valueOf(scheduleId), JOB_GROUP);
            return scheduler.checkExists(jobKey);
        } catch (SchedulerException e) {
            return false;
        }
    }

    @Override
    public void loadAllJobs() {
        try {
            List<ReportSchedule> scheduleList = reportScheduleService.list();
            if (scheduleList == null || scheduleList.isEmpty()) {
                logger.info("没有可加载的调度任务");
                return;
            }
            int enabledCount = 0;
            for (ReportSchedule schedule : scheduleList) {
                try {
                    addJob(schedule);
                    if (schedule.getStatus() != null && schedule.getStatus() == 0) {
                        pauseJob(schedule.getId());
                    } else {
                        enabledCount++;
                    }
                } catch (Exception e) {
                    logger.error("加载调度任务失败，scheduleId: {}", schedule.getId(), e);
                }
            }
            logger.info("已加载全部调度任务，共{}个，启用{}个", scheduleList.size(), enabledCount);
        } catch (Exception e) {
            logger.error("加载全部调度任务失败", e);
        }
    }

    private JobDetail buildJobDetail(ReportSchedule schedule) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("scheduleId", schedule.getId());

        return JobBuilder.newJob(ReportQuartzJob.class)
                .withIdentity(String.valueOf(schedule.getId()), JOB_GROUP)
                .withDescription(schedule.getName())
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private CronTrigger buildTrigger(ReportSchedule schedule) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule(schedule.getCronExpression())
                .withMisfireHandlingInstructionDoNothing();

        return TriggerBuilder.newTrigger()
                .withIdentity(String.valueOf(schedule.getId()), TRIGGER_GROUP)
                .withSchedule(scheduleBuilder)
                .build();
    }
}
