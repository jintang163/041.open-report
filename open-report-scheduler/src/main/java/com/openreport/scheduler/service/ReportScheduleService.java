package com.openreport.scheduler.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.scheduler.entity.ReportSchedule;

import java.util.List;

public interface ReportScheduleService extends IService<ReportSchedule> {

    Page<ReportSchedule> pageList(Integer pageNum, Integer pageSize, Long reportId, Integer status);

    List<ReportSchedule> listEnabled();

    boolean trigger(Long id);

    boolean enable(Long id);

    boolean disable(Long id);
}
