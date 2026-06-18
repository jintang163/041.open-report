package com.openreport.scheduler.listener;

import com.openreport.scheduler.service.ScheduleJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ScheduleJobLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleJobLoader.class);

    @Autowired
    private ScheduleJobManager scheduleJobManager;

    @Override
    public void run(String... args) {
        logger.info("开始加载全部调度任务到 Quartz...");
        try {
            Thread.sleep(1000);
            scheduleJobManager.loadAllJobs();
            logger.info("调度任务加载完成");
        } catch (Exception e) {
            logger.error("加载调度任务失败", e);
        }
    }
}
