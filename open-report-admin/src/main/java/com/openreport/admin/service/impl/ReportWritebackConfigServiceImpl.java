package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.dto.writeback.WritebackConfigDTO;
import com.openreport.admin.entity.ReportWritebackConfig;
import com.openreport.admin.entity.ReportWritebackField;
import com.openreport.admin.mapper.ReportWritebackConfigMapper;
import com.openreport.admin.mapper.ReportWritebackFieldMapper;
import com.openreport.admin.service.ReportWritebackConfigService;
import com.openreport.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportWritebackConfigServiceImpl extends ServiceImpl<ReportWritebackConfigMapper, ReportWritebackConfig> implements ReportWritebackConfigService {

    @Autowired
    private ReportWritebackFieldMapper fieldMapper;

    @Override
    public List<ReportWritebackConfig> getByReportId(Long reportId) {
        List<ReportWritebackConfig> configs = baseMapper.selectByReportId(reportId);
        if (!CollectionUtils.isEmpty(configs)) {
            for (ReportWritebackConfig config : configs) {
                config.setFields(fieldMapper.selectByConfigId(config.getId()));
            }
        }
        return configs;
    }

    @Override
    public ReportWritebackConfig getDetailById(Long id) {
        ReportWritebackConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("回写配置不存在");
        }
        config.setFields(fieldMapper.selectByConfigId(id));
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(WritebackConfigDTO dto) {
        ReportWritebackConfig config = new ReportWritebackConfig();
        config.setReportId(dto.getReportId());
        config.setDataSourceId(dto.getDataSourceId());
        config.setTableName(dto.getTableName());
        config.setPrimaryKeyField(dto.getPrimaryKeyField());
        config.setPrimaryKeyColumn(dto.getPrimaryKeyColumn());
        config.setVersionField(dto.getVersionField());
        config.setLogicDeleteField(dto.getLogicDeleteField());
        config.setLogicDeleteValue(dto.getLogicDeleteValue());
        config.setLogicNotDeleteValue(dto.getLogicNotDeleteValue());
        config.setBatchSupport(dto.getBatchSupport() != null ? dto.getBatchSupport() : 1);
        config.setTransactionEnable(dto.getTransactionEnable() != null ? dto.getTransactionEnable() : 1);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setDeleted(0);
        save(config);

        saveFields(config.getId(), dto.getFields());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(WritebackConfigDTO dto) {
        ReportWritebackConfig config = getById(dto.getId());
        if (config == null) {
            throw new BusinessException("回写配置不存在");
        }

        config.setDataSourceId(dto.getDataSourceId());
        config.setTableName(dto.getTableName());
        config.setPrimaryKeyField(dto.getPrimaryKeyField());
        config.setPrimaryKeyColumn(dto.getPrimaryKeyColumn());
        config.setVersionField(dto.getVersionField());
        config.setLogicDeleteField(dto.getLogicDeleteField());
        config.setLogicDeleteValue(dto.getLogicDeleteValue());
        config.setLogicNotDeleteValue(dto.getLogicNotDeleteValue());
        config.setBatchSupport(dto.getBatchSupport() != null ? dto.getBatchSupport() : 1);
        config.setTransactionEnable(dto.getTransactionEnable() != null ? dto.getTransactionEnable() : 1);
        config.setUpdateTime(LocalDateTime.now());
        updateById(config);

        fieldMapper.deleteByConfigId(config.getId());
        saveFields(config.getId(), dto.getFields());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        ReportWritebackConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("回写配置不存在");
        }
        removeById(id);
        fieldMapper.deleteByConfigId(id);
    }

    private void saveFields(Long configId, List<WritebackConfigDTO.FieldMappingDTO> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }

        List<ReportWritebackField> fieldList = new ArrayList<>();
        for (WritebackConfigDTO.FieldMappingDTO fieldDTO : fields) {
            ReportWritebackField field = new ReportWritebackField();
            field.setConfigId(configId);
            field.setCellPosition(fieldDTO.getCellPosition());
            field.setFieldName(fieldDTO.getFieldName());
            field.setFieldType(fieldDTO.getFieldType() != null ? fieldDTO.getFieldType() : "STRING");
            field.setEditable(fieldDTO.getEditable() != null ? fieldDTO.getEditable() : 1);
            field.setRequired(fieldDTO.getRequired() != null ? fieldDTO.getRequired() : 0);
            field.setDefaultValue(fieldDTO.getDefaultValue());
            field.setValidationRule(fieldDTO.getValidationRule());
            field.setValidationMessage(fieldDTO.getValidationMessage());
            field.setCreateTime(LocalDateTime.now());
            field.setUpdateTime(LocalDateTime.now());
            field.setDeleted(0);
            fieldList.add(field);
        }

        for (ReportWritebackField field : fieldList) {
            fieldMapper.insert(field);
        }
    }
}
