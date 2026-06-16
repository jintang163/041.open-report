package com.openreport.scheduler.job;

import com.openreport.scheduler.service.ReportLogService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportCleanupJob {

    private static Logger logger = LoggerFactory.getLogger(ReportCleanupJob.class);

    @Value("${report.log.cleanup.days:30}")
    private Integer cleanupDays;

    @Autowired
    private ReportLogService reportLogService;

    @XxlJob("reportCleanupJob")
    public void reportCleanupJob() {
        logger.info("报表日志清理任务开始执行，清理天数：{}", cleanupDays);
        try {
            reportLogService.cleanupOldLogs(cleanupDays);
            XxlJobHelper.handleSuccess("报表日志清理任务执行完成");
            logger.info("报表日志清理任务执行完成");
        } catch (Exception e) {
            logger.error("报表日志清理任务执行异常", e);
            XxlJobHelper.handleFail("报表日志清理任务执行异常: " + e.getMessage());
        }
    }
}
