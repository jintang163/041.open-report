package com.openreport.scheduler.job;

import com.openreport.scheduler.entity.ReportSchedule;
import com.openreport.scheduler.service.ReportScheduleService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportExecuteJob {

    private static Logger logger = LoggerFactory.getLogger(ReportExecuteJob.class);

    @Autowired
    private ReportScheduleService reportScheduleService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @XxlJob("reportExecuteJob")
    public void reportExecuteJob() {
        String jobParam = XxlJobHelper.getJobParam();
        logger.info("报表执行定时任务开始执行，参数：{}", jobParam);
        try {
            List<ReportSchedule> scheduleList = reportScheduleService.listEnabled();
            if (scheduleList == null || scheduleList.isEmpty()) {
                XxlJobHelper.log("没有需要执行的报表调度任务");
                return;
            }
            for (ReportSchedule schedule : scheduleList) {
                try {
                    executeSchedule(schedule);
                } catch (Exception e) {
                    logger.error("执行报表调度失败，scheduleId: {}", schedule.getId(), e);
                    XxlJobHelper.log("执行报表调度失败，scheduleId: " + schedule.getId() + ", error: " + e.getMessage());
                }
            }
            XxlJobHelper.handleSuccess("报表执行定时任务执行完成");
        } catch (Exception e) {
            logger.error("报表执行定时任务执行异常", e);
            XxlJobHelper.handleFail("报表执行定时任务执行异常: " + e.getMessage());
        }
    }

    private void executeSchedule(ReportSchedule schedule) {
        Map<String, Object> message = new HashMap<>();
        message.put("scheduleId", schedule.getId());
        message.put("reportId", schedule.getReportId());
        message.put("params", schedule.getParams());
        message.put("outputType", schedule.getOutputType());
        message.put("emailList", schedule.getEmailList());
        message.put("executeType", "SCHEDULE");
        kafkaTemplate.send("report-execute-topic", com.alibaba.fastjson.JSON.toJSONString(message));
        schedule.setLastExecuteTime(LocalDateTime.now());
        reportScheduleService.updateById(schedule);
        logger.info("已发送报表执行消息，scheduleId: {}, reportId: {}", schedule.getId(), schedule.getReportId());
    }
}
