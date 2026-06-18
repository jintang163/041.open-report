package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openreport.admin.dto.TemplateVersionDiffDTO;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateSnapshot;
import com.openreport.admin.mapper.ReportTemplateMapper;
import com.openreport.admin.mapper.ReportTemplateSnapshotMapper;
import com.openreport.admin.service.ReportTemplateSnapshotService;
import com.openreport.common.enums.ReportStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ReportTemplateSnapshotServiceImpl extends ServiceImpl<ReportTemplateSnapshotMapper, ReportTemplateSnapshot>
        implements ReportTemplateSnapshotService {

    @Autowired
    private ReportTemplateMapper reportTemplateMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ReportTemplateSnapshot> listByTemplateId(Long templateId) {
        return baseMapper.listByTemplateId(templateId);
    }

    @Override
    public ReportTemplateSnapshot getByVersion(Long templateId, Integer version) {
        return baseMapper.getByVersion(templateId, version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateSnapshot createSnapshot(Long templateId, Long userId, String userName, String changeLog) {
        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        Integer currentMaxVersion = baseMapper.getMaxVersion(templateId);
        int newVersion = currentMaxVersion == null ? 1 : currentMaxVersion + 1;

        ReportTemplateSnapshot snapshot = new ReportTemplateSnapshot();
        snapshot.setTemplateId(templateId);
        snapshot.setVersion(newVersion);
        snapshot.setTemplateName(template.getTemplateName());
        snapshot.setTemplateJson(template.getTemplateJson());
        snapshot.setDataSetBind(template.getDataSetBind());
        snapshot.setParamConfig(template.getParamConfig());
        snapshot.setDescription(template.getDescription());
        snapshot.setChangeLog(changeLog);
        snapshot.setStatus(template.getStatus());
        snapshot.setCreateBy(userId);
        snapshot.setCreateByName(userName);
        snapshot.setCreateTime(LocalDateTime.now());

        baseMapper.insert(snapshot);
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateSnapshot rollbackToVersion(Long templateId, Integer version, Long userId, String userName) {
        ReportTemplateSnapshot targetSnapshot = baseMapper.getByVersion(templateId, version);
        if (targetSnapshot == null) {
            throw new RuntimeException("目标版本不存在");
        }

        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        template.setTemplateName(targetSnapshot.getTemplateName());
        template.setTemplateJson(targetSnapshot.getTemplateJson());
        template.setDataSetBind(targetSnapshot.getDataSetBind());
        template.setParamConfig(targetSnapshot.getParamConfig());
        template.setDescription(targetSnapshot.getDescription());
        template.setStatus(ReportStatusEnum.DRAFT.getCode());
        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateMapper.updateById(template);

        Integer currentMaxVersion = baseMapper.getMaxVersion(templateId);
        int newVersion = currentMaxVersion == null ? 1 : currentMaxVersion + 1;

        ReportTemplateSnapshot rollbackSnapshot = new ReportTemplateSnapshot();
        rollbackSnapshot.setTemplateId(templateId);
        rollbackSnapshot.setVersion(newVersion);
        rollbackSnapshot.setTemplateName(targetSnapshot.getTemplateName());
        rollbackSnapshot.setTemplateJson(targetSnapshot.getTemplateJson());
        rollbackSnapshot.setDataSetBind(targetSnapshot.getDataSetBind());
        rollbackSnapshot.setParamConfig(targetSnapshot.getParamConfig());
        rollbackSnapshot.setDescription(targetSnapshot.getDescription());
        rollbackSnapshot.setChangeLog("回滚到版本 v" + version);
        rollbackSnapshot.setStatus(ReportStatusEnum.DRAFT.getCode());
        rollbackSnapshot.setCreateBy(userId);
        rollbackSnapshot.setCreateByName(userName);
        rollbackSnapshot.setCreateTime(LocalDateTime.now());

        baseMapper.insert(rollbackSnapshot);
        return rollbackSnapshot;
    }

    @Override
    public Integer getMaxVersion(Long templateId) {
        return baseMapper.getMaxVersion(templateId);
    }

    @Override
    public TemplateVersionDiffDTO compareVersions(Long templateId, Integer baseVersion, Integer targetVersion) {
        ReportTemplateSnapshot baseSnapshot = getByVersion(templateId, baseVersion);
        ReportTemplateSnapshot targetSnapshot = getByVersion(templateId, targetVersion);

        if (baseSnapshot == null || targetSnapshot == null) {
            throw new RuntimeException("版本不存在");
        }

        TemplateVersionDiffDTO diffDTO = new TemplateVersionDiffDTO();
        diffDTO.setTemplateId(templateId);
        diffDTO.setBaseVersion(baseVersion);
        diffDTO.setTargetVersion(targetVersion);
        diffDTO.setBaseVersionName(baseSnapshot.getTemplateName());
        diffDTO.setTargetVersionName(targetSnapshot.getTemplateName());
        diffDTO.setBaseCreateByName(baseSnapshot.getCreateByName());
        diffDTO.setTargetCreateByName(targetSnapshot.getCreateByName());
        diffDTO.setBaseCreateTime(baseSnapshot.getCreateTime());
        diffDTO.setTargetCreateTime(targetSnapshot.getCreateTime());

        List<TemplateVersionDiffDTO.DiffItem> diffItems = new ArrayList<>();

        compareField(diffItems, "templateName", "模板名称",
                baseSnapshot.getTemplateName(), targetSnapshot.getTemplateName());
        compareField(diffItems, "description", "描述",
                baseSnapshot.getDescription(), targetSnapshot.getDescription());
        compareField(diffItems, "dataSetBind", "数据集绑定",
                baseSnapshot.getDataSetBind(), targetSnapshot.getDataSetBind());
        compareField(diffItems, "paramConfig", "参数配置",
                baseSnapshot.getParamConfig(), targetSnapshot.getParamConfig());
        compareField(diffItems, "changeLog", "变更说明",
                baseSnapshot.getChangeLog(), targetSnapshot.getChangeLog());
        compareJsonField(diffItems, "templateJson", "模板内容",
                baseSnapshot.getTemplateJson(), targetSnapshot.getTemplateJson());

        diffDTO.setDiffItems(diffItems);
        return diffDTO;
    }

    @Override
    public ReportTemplateSnapshot getLatestPublishedVersion(Long templateId) {
        LambdaQueryWrapper<ReportTemplateSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplateSnapshot::getTemplateId, templateId)
                .eq(ReportTemplateSnapshot::getStatus, ReportStatusEnum.PUBLISHED.getCode())
                .orderByDesc(ReportTemplateSnapshot::getVersion)
                .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public ReportTemplateSnapshot previewPublish(Long templateId) {
        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        ReportTemplateSnapshot previewSnapshot = new ReportTemplateSnapshot();
        previewSnapshot.setTemplateId(templateId);
        previewSnapshot.setVersion(0);
        previewSnapshot.setTemplateName(template.getTemplateName());
        previewSnapshot.setTemplateJson(template.getTemplateJson());
        previewSnapshot.setDataSetBind(template.getDataSetBind());
        previewSnapshot.setParamConfig(template.getParamConfig());
        previewSnapshot.setDescription(template.getDescription());
        previewSnapshot.setChangeLog("发布预览");
        previewSnapshot.setStatus(template.getStatus());
        previewSnapshot.setCreateTime(LocalDateTime.now());

        return previewSnapshot;
    }

    private void compareField(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                              String fieldName, String fieldLabel,
                              String baseValue, String targetValue) {
        String baseStr = baseValue != null ? baseValue : "";
        String targetStr = targetValue != null ? targetValue : "";

        if (!Objects.equals(baseStr, targetStr)) {
            String diffType;
            if (baseStr.isEmpty() && !targetStr.isEmpty()) {
                diffType = "ADD";
            } else if (!baseStr.isEmpty() && targetStr.isEmpty()) {
                diffType = "DELETE";
            } else {
                diffType = "MODIFY";
            }
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    fieldName, fieldLabel, baseValue, targetValue, diffType
            ));
        }
    }

    private void compareJsonField(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                  String fieldName, String fieldLabel,
                                  String baseJson, String targetJson) {
        String baseStr = baseJson != null ? baseJson : "";
        String targetStr = targetJson != null ? targetJson : "";

        if (baseStr.isEmpty() && targetStr.isEmpty()) {
            return;
        }

        if (baseStr.isEmpty() || targetStr.isEmpty()) {
            compareField(diffItems, fieldName, fieldLabel, baseJson, targetJson);
            return;
        }

        try {
            JsonNode baseNode = objectMapper.readTree(baseStr);
            JsonNode targetNode = objectMapper.readTree(targetStr);

            if (!baseNode.equals(targetNode)) {
                diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                        fieldName, fieldLabel, baseJson, targetJson, "MODIFY"
                ));
            }
        } catch (JsonProcessingException e) {
            if (!baseStr.equals(targetStr)) {
                diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                        fieldName, fieldLabel, baseJson, targetJson, "MODIFY"
                ));
            }
        }
    }
}
