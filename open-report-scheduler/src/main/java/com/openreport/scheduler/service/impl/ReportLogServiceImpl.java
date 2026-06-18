package com.openreport.scheduler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.scheduler.entity.ReportLog;
import com.openreport.scheduler.mapper.ReportLogMapper;
import com.openreport.scheduler.service.ReportLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReportLogServiceImpl extends ServiceImpl<ReportLogMapper, ReportLog> implements ReportLogService {

    @Override
    public Page<ReportLog> pageList(Integer pageNum, Integer pageSize, Long reportId, Long scheduleId, String status, String executeType) {
        Page<ReportLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportLog> wrapper = new LambdaQueryWrapper<>();
        if (reportId != null) {
            wrapper.eq(ReportLog::getReportId, reportId);
        }
        if (scheduleId != null) {
            wrapper.eq(ReportLog::getScheduleId, scheduleId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ReportLog::getStatus, status);
        }
        if (executeType != null && !executeType.isEmpty()) {
            wrapper.eq(ReportLog::getExecuteType, executeType);
        }
        wrapper.orderByDesc(ReportLog::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public ReportLog createLog(Long reportId, Long scheduleId, String executeType, String params, Integer retryCount) {
        ReportLog log = new ReportLog();
        log.setReportId(reportId);
        log.setScheduleId(scheduleId);
        log.setExecuteType(executeType);
        log.setParams(params);
        log.setStatus("RUNNING");
        log.setRetryCount(retryCount == null ? 0 : retryCount);
        log.setCreateTime(LocalDateTime.now());
        log.setDeleted(0);
        save(log);
        return log;
    }

    @Override
    public void updateLogSuccess(Long logId, Long costTime, String outputPath) {
        ReportLog log = getById(logId);
        if (log != null) {
            log.setStatus("SUCCESS");
            log.setCostTime(costTime);
            log.setOutputPath(outputPath);
            updateById(log);
        }
    }

    @Override
    public void updateLogFail(Long logId, Long costTime, String errorMsg) {
        ReportLog log = getById(logId);
        if (log != null) {
            log.setStatus("FAIL");
            log.setCostTime(costTime);
            log.setErrorMsg(errorMsg);
            updateById(log);
        }
    }

    @Override
    public void updateLogRunning(Long logId) {
        ReportLog log = getById(logId);
        if (log != null) {
            log.setStatus("RUNNING");
            updateById(log);
        }
    }

    @Override
    public void cleanupOldLogs(Integer days) {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<ReportLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(ReportLog::getCreateTime, expireTime);
        remove(wrapper);
    }
}
