package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportTemplate;

import java.util.List;

public interface ReportTemplateService extends IService<ReportTemplate> {

    Page<ReportTemplate> pageList(Integer pageNum, Integer pageSize, String templateName, Integer templateType);

    Page<ReportTemplate> pageListV2(Integer pageNum, Integer pageSize, String keyword, Integer status);

    List<ReportTemplate> listAll();

    ReportTemplate copyTemplate(Long id, Long userId, String userName);

    ReportTemplate saveDraft(ReportTemplate template, Long userId, String userName);
}
