package com.openreport.scheduler.service;

import com.openreport.scheduler.entity.ReportSchedule;

public interface ScheduleJobManager {

    void addJob(ReportSchedule schedule);

    void updateJob(ReportSchedule schedule);

    void deleteJob(Long scheduleId);

    void pauseJob(Long scheduleId);

    void resumeJob(Long scheduleId);

    void triggerJob(Long scheduleId);

    boolean checkExists(Long scheduleId);

    void loadAllJobs();
}
