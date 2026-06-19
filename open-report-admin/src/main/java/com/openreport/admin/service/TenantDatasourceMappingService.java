package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.entity.TenantDatasourceMapping;

import java.util.List;

public interface TenantDatasourceMappingService extends IService<TenantDatasourceMapping> {

    DataSourceConfig resolveDatasource(Long tenantId, Long originalDsId);

    DataSourceConfig resolveDatasourceForCurrentUser(Long originalDsId);

    List<TenantDatasourceMapping> listByTenantId(Long tenantId);

    TenantDatasourceMapping getByTenantAndOriginalDs(Long tenantId, Long originalDsId);

    void saveMapping(Long tenantId, Long originalDsId, Long targetDsId, Long operateUserId);

    void updateMapping(Long id, Long targetDsId, Long operateUserId);

    void deleteMapping(Long id);

    void removeByTenantAndOriginalDs(Long tenantId, Long originalDsId);
}
