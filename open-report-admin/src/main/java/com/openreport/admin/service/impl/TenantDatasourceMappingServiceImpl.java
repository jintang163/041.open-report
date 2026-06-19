package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.entity.TenantDatasourceMapping;
import com.openreport.admin.mapper.TenantDatasourceMappingMapper;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.admin.service.TenantDatasourceMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TenantDatasourceMappingServiceImpl
        extends ServiceImpl<TenantDatasourceMappingMapper, TenantDatasourceMapping>
        implements TenantDatasourceMappingService {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Override
    public DataSourceConfig resolveDatasource(Long tenantId, Long originalDsId) {
        if (tenantId == null || originalDsId == null) {
            return dataSourceConfigService.getById(originalDsId);
        }

        TenantDatasourceMapping mapping = getByTenantAndOriginalDs(tenantId, originalDsId);
        if (mapping != null && mapping.getTargetDsId() != null) {
            DataSourceConfig targetDs = dataSourceConfigService.getById(mapping.getTargetDsId());
            if (targetDs != null && targetDs.getStatus() != null && targetDs.getStatus() == 1) {
                log.debug("Tenant datasource resolved: tenantId={}, originalDsId={} -> targetDsId={}",
                        tenantId, originalDsId, mapping.getTargetDsId());
                return targetDs;
            }
            log.warn("Tenant datasource mapping target is invalid: tenantId={}, originalDsId={}, targetDsId={}",
                    tenantId, originalDsId, mapping.getTargetDsId());
        }

        return dataSourceConfigService.getById(originalDsId);
    }

    @Override
    public DataSourceConfig resolveDatasourceForCurrentUser(Long originalDsId) {
        Long tenantId = SecurityContextHolder.getTenantId();
        return resolveDatasource(tenantId, originalDsId);
    }

    @Override
    public List<TenantDatasourceMapping> listByTenantId(Long tenantId) {
        LambdaQueryWrapper<TenantDatasourceMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDatasourceMapping::getTenantId, tenantId);
        wrapper.orderByAsc(TenantDatasourceMapping::getOriginalDsId);
        return list(wrapper);
    }

    @Override
    public TenantDatasourceMapping getByTenantAndOriginalDs(Long tenantId, Long originalDsId) {
        LambdaQueryWrapper<TenantDatasourceMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDatasourceMapping::getTenantId, tenantId);
        wrapper.eq(TenantDatasourceMapping::getOriginalDsId, originalDsId);
        wrapper.eq(TenantDatasourceMapping::getStatus, 1);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public void saveMapping(Long tenantId, Long originalDsId, Long targetDsId, Long operateUserId) {
        TenantDatasourceMapping existing = getByTenantAndOriginalDs(tenantId, originalDsId);
        if (existing != null) {
            existing.setTargetDsId(targetDsId);
            existing.setUpdateBy(operateUserId);
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
            log.info("Updated tenant datasource mapping: tenantId={}, originalDsId={} -> targetDsId={}",
                    tenantId, originalDsId, targetDsId);
            return;
        }

        TenantDatasourceMapping mapping = new TenantDatasourceMapping();
        mapping.setTenantId(tenantId);
        mapping.setOriginalDsId(originalDsId);
        mapping.setTargetDsId(targetDsId);
        mapping.setStatus(1);
        mapping.setCreateBy(operateUserId);
        mapping.setCreateTime(LocalDateTime.now());
        mapping.setUpdateBy(operateUserId);
        mapping.setUpdateTime(LocalDateTime.now());
        save(mapping);
        log.info("Created tenant datasource mapping: tenantId={}, originalDsId={} -> targetDsId={}",
                tenantId, originalDsId, targetDsId);
    }

    @Override
    public void updateMapping(Long id, Long targetDsId, Long operateUserId) {
        TenantDatasourceMapping mapping = getById(id);
        if (mapping == null) {
            throw new IllegalArgumentException("映射记录不存在");
        }
        mapping.setTargetDsId(targetDsId);
        mapping.setUpdateBy(operateUserId);
        mapping.setUpdateTime(LocalDateTime.now());
        updateById(mapping);
    }

    @Override
    public void deleteMapping(Long id) {
        removeById(id);
    }

    @Override
    public void removeByTenantAndOriginalDs(Long tenantId, Long originalDsId) {
        LambdaQueryWrapper<TenantDatasourceMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDatasourceMapping::getTenantId, tenantId);
        wrapper.eq(TenantDatasourceMapping::getOriginalDsId, originalDsId);
        remove(wrapper);
    }
}
