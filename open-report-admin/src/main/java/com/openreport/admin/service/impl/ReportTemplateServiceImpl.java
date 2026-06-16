package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.mapper.ReportTemplateMapper;
import com.openreport.admin.service.ReportTemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportTemplateServiceImpl extends ServiceImpl<ReportTemplateMapper, ReportTemplate> implements ReportTemplateService {

    @Override
    public Page<ReportTemplate> pageList(Integer pageNum, Integer pageSize, String templateName, Integer templateType) {
        Page<ReportTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(templateName)) {
            wrapper.like(ReportTemplate::getTemplateName, templateName);
        }
        if (templateType != null) {
            wrapper.eq(ReportTemplate::getTemplateType, templateType);
        }
        wrapper.orderByDesc(ReportTemplate::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<ReportTemplate> listAll() {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getStatus, 1);
        wrapper.orderByAsc(ReportTemplate::getTemplateName);
        return list(wrapper);
    }
}
