package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportTemplate;

import java.util.List;

public interface ReportTemplateService extends IService<ReportTemplate> {

    Page<ReportTemplate> pageList(Integer pageNum, Integer pageSize, String templateName, Integer templateType);

    List<ReportTemplate> listAll();
}
