package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.dto.TemplateEditLockInfo;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.mapper.ReportTemplateMapper;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.admin.service.ReportTemplateSnapshotService;
import com.openreport.admin.service.TemplateEditLockService;
import com.openreport.admin.service.DataLineageService;
import com.openreport.common.enums.ReportStatusEnum;
import com.openreport.common.result.ResultCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReportTemplateServiceImpl extends ServiceImpl<ReportTemplateMapper, ReportTemplate> implements ReportTemplateService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportTemplateServiceImpl.class);

    @Autowired
    private ReportTemplateSnapshotService snapshotService;

    @Autowired
    private com.openreport.admin.websocket.WebSocketPushService pushService;

    @Autowired
    private TemplateEditLockService templateEditLockService;

    @Autowired
    private DataLineageService dataLineageService;

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
    public Page<ReportTemplate> pageListV2(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<ReportTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ReportTemplate::getTemplateName, keyword)
                    .or().like(ReportTemplate::getTemplateCode, keyword));
        }
        if (status != null) {
            wrapper.eq(ReportTemplate::getStatus, status);
        }
        wrapper.orderByDesc(ReportTemplate::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<ReportTemplate> listAll() {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getStatus, ReportStatusEnum.PUBLISHED.getCode());
        wrapper.orderByAsc(ReportTemplate::getTemplateName);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplate copyTemplate(Long id, Long userId, String userName) {
        ReportTemplate sourceTemplate = baseMapper.selectById(id);
        if (sourceTemplate == null) {
            throw new RuntimeException("源模板不存在");
        }
        templateEditLockService.checkLockOrThrow(id, userId, null);

        ReportTemplate newTemplate = new ReportTemplate();
        newTemplate.setTemplateName(sourceTemplate.getTemplateName() + "_副本");
        newTemplate.setTemplateCode(sourceTemplate.getTemplateCode() + "_" + UUID.randomUUID().toString().substring(0, 8));
        newTemplate.setTemplateType(sourceTemplate.getTemplateType());
        newTemplate.setTemplateJson(sourceTemplate.getTemplateJson());
        newTemplate.setDataSetBind(sourceTemplate.getDataSetBind());
        newTemplate.setParamConfig(sourceTemplate.getParamConfig());
        newTemplate.setDescription(sourceTemplate.getDescription() + "（复制自：" + sourceTemplate.getTemplateName() + "）");
        newTemplate.setStatus(ReportStatusEnum.DRAFT.getCode());
        newTemplate.setCreateBy(userId);
        newTemplate.setCreateTime(LocalDateTime.now());
        newTemplate.setUpdateBy(userId);
        newTemplate.setUpdateTime(LocalDateTime.now());
        newTemplate.setDeleted(0);

        baseMapper.insert(newTemplate);

        snapshotService.createSnapshot(newTemplate.getId(), userId, userName, "模板复制创建");
        pushService.pushTemplateChange(newTemplate, "COPY");

        return newTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplate saveDraft(ReportTemplate template, Long userId, String userName) {
        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());

        boolean isNew = template.getId() == null;
        if (isNew) {
            template.setCreateBy(userId);
            template.setCreateTime(LocalDateTime.now());
            template.setDeleted(0);
            if (template.getStatus() == null) {
                template.setStatus(ReportStatusEnum.DRAFT.getCode());
            }
            if (StringUtils.isBlank(template.getTemplateCode())) {
                template.setTemplateCode("TPL_" + System.currentTimeMillis());
            }
            baseMapper.insert(template);
            snapshotService.createSnapshot(template.getId(), userId, userName, "创建草稿");
        } else {
            ReportTemplate existingTemplate = baseMapper.selectById(template.getId());
            if (existingTemplate == null) {
                throw new RuntimeException("模板不存在");
            }
            if (ReportStatusEnum.PUBLISHED.getCode().equals(existingTemplate.getStatus())) {
                template.setStatus(ReportStatusEnum.DRAFT.getCode());
            }
            baseMapper.updateById(template);
            snapshotService.createSnapshot(template.getId(), userId, userName, "更新草稿");
        }

        pushService.pushTemplateChange(template, isNew ? "INSERT" : "UPDATE");

        try {
            dataLineageService.refreshLineageForReport(template.getId());
        } catch (Exception e) {
            logger.warn("自动刷新血缘关系失败（不影响保存）: reportId={}", template.getId(), e);
        }

        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplate saveDraftWithLock(ReportTemplate template, Long userId, String userName, String lockToken) {
        templateEditLockService.checkLockOrThrow(template.getId(), userId, lockToken);
        if (template.getId() != null) {
            templateEditLockService.renewLock(template.getId(), userId, lockToken);
        }
        return saveDraft(template, userId, userName);
    }

    @Override
    public TemplateEditLockInfo enterEdit(Long templateId, Long userId, String userName) {
        ReportTemplate template = baseMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException(ResultCode.DATA_NOT_FOUND.getMessage());
        }
        return templateEditLockService.acquireLock(templateId, userId, userName);
    }

    @Override
    public boolean leaveEdit(Long templateId, Long userId, String lockToken) {
        return templateEditLockService.releaseLock(templateId, userId, lockToken);
    }

    @Override
    public boolean heartbeat(Long templateId, Long userId, String lockToken) {
        return templateEditLockService.renewLock(templateId, userId, lockToken);
    }

    @Override
    public TemplateEditLockInfo getLockStatus(Long templateId) {
        return templateEditLockService.getLockInfo(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIdWithLock(Long id, Long userId, String lockToken) {
        templateEditLockService.checkLockOrThrow(id, userId, lockToken);
        ReportTemplate template = baseMapper.selectById(id);
        if (template != null) {
            baseMapper.deleteById(id);
            pushService.pushTemplateChange(template, "DELETE");
        }
    }

    @Override
    public void checkLockOrThrow(Long templateId, Long userId, String lockToken) {
        templateEditLockService.checkLockOrThrow(templateId, userId, lockToken);
    }
}
