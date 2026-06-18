package com.openreport.scheduler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.scheduler.entity.ReportSchedule;
import com.openreport.scheduler.mapper.ReportScheduleMapper;
import com.openreport.scheduler.service.ReportLogService;
import com.openreport.scheduler.service.ReportScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportScheduleServiceImpl extends ServiceImpl<ReportScheduleMapper, ReportSchedule> implements ReportScheduleService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ReportLogService reportLogService;

    @Override
    public Page<ReportSchedule> pageList(Integer pageNum, Integer pageSize, Long reportId, Integer status) {
        Page<ReportSchedule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<>();
        if (reportId != null) {
            wrapper.eq(ReportSchedule::getReportId, reportId);
        }
        if (status != null) {
            wrapper.eq(ReportSchedule::getStatus, status);
        }
        wrapper.orderByDesc(ReportSchedule::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<ReportSchedule> listEnabled() {
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSchedule::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public boolean trigger(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            return false;
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
        message.put("executeType", "MANUAL");
        try {
            kafkaTemplate.send("report-execute-topic", com.alibaba.fastjson.JSON.toJSONString(message));
            schedule.setLastExecuteTime(LocalDateTime.now());
            updateById(schedule);
            return true;
        } catch (Exception e) {
            log.error("触发报表执行失败", e);
            return false;
        }
    }

    @Override
    public boolean enable(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            return false;
        }
        schedule.setStatus(1);
        return updateById(schedule);
    }

    @Override
    public boolean disable(Long id) {
        ReportSchedule schedule = getById(id);
        if (schedule == null) {
            return false;
        }
        schedule.setStatus(0);
        return updateById(schedule);
    }
}
