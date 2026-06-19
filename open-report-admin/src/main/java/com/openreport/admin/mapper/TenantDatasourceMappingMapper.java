package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.TenantDatasourceMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TenantDatasourceMappingMapper extends BaseMapper<TenantDatasourceMapping> {

    @Select("SELECT * FROM tenant_datasource_mapping WHERE tenant_id = #{tenantId} AND original_ds_id = #{originalDsId} AND status = 1 LIMIT 1")
    TenantDatasourceMapping findByTenantAndOriginalDs(@Param("tenantId") Long tenantId, @Param("originalDsId") Long originalDsId);
}
