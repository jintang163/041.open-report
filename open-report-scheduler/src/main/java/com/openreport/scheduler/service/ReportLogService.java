package com.openreport.scheduler.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.scheduler.entity.ReportLog;

import java.time.LocalDateTime;

public interface ReportLogService extends IService<ReportLog> {

    Page<ReportLog> pageList(Integer pageNum, Integer pageSize, Long reportId, Long scheduleId, String status, String executeType);

    ReportLog createLog(Long reportId, Long scheduleId, String executeType, String params, Integer retryCount);

    void updateLogSuccess(Long logId, Long costTime, String outputPath);

    void updateLogFail(Long logId, Long costTime, String errorMsg);

    void updateLogRunning(Long logId);

    void cleanupOldLogs(Integer days);
}
