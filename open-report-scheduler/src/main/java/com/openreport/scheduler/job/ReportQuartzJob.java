package com.openreport.scheduler.job;

import com.alibaba.fastjson.JSON;
import com.openreport.scheduler.entity.ReportSchedule;
import com.openreport.scheduler.service.ReportScheduleService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ReportQuartzJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ReportQuartzJob.class);

    @Autowired
    private ReportScheduleService reportScheduleService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long scheduleId = context.getJobDetail().getJobDataMap().getLong("scheduleId");
        logger.info("Quartz 定时任务触发，scheduleId: {}", scheduleId);

        try {
            ReportSchedule schedule = reportScheduleService.getById(scheduleId);
            if (schedule == null) {
                logger.warn("调度任务不存在，scheduleId: {}", scheduleId);
                return;
            }
            if (schedule.getStatus() == null || schedule.getStatus() != 1) {
                logger.warn("调度任务已停用，跳过执行，scheduleId: {}", scheduleId);
                return;
            }

            Map<String, Object> message = new HashMap<>();
            message.put("scheduleId", schedule.getId());
            message.put("reportId", schedule.getReportId());
            message.put("params", schedule.getParams());
            message.put("outputType", schedule.getOutputType());
            message.put("emailList", schedule.getEmailList());
            message.put("emailCcList", schedule.getEmailCcList());
            message.put("emailSubject", schedule.getEmailSubject());
            message.put("emailContent", schedule.getEmailContent());
            message.put("retryCount", 0);
            message.put("executeType", "SCHEDULE");

            kafkaTemplate.send("report-execute-topic", JSON.toJSONString(message));

            schedule.setLastExecuteTime(LocalDateTime.now());
            reportScheduleService.updateById(schedule);

            logger.info("已发送报表执行消息，scheduleId: {}, reportId: {}", schedule.getId(), schedule.getReportId());
        } catch (Exception e) {
            logger.error("Quartz 定时任务执行失败，scheduleId: {}", scheduleId, e);
            throw new JobExecutionException(e);
        }
    }
}
