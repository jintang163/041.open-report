package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.DataSourceConfig;

import java.util.List;
import java.util.Map;

public interface DataSourceConfigService extends IService<DataSourceConfig> {

    Page<DataSourceConfig> pageList(Integer pageNum, Integer pageSize, String dsName, String dsType);

    List<DataSourceConfig> listAll();

    boolean testConnection(DataSourceConfig config);

    Map<String, Object> getConnectionInfo(Long id);

    List<Map<String, Object>> getTables(Long dsId);

    List<Map<String, Object>> getTableColumns(Long dsId, String tableName);

    String generateSchemaInfo(Long dsId);

    Map<String, Object> validateSql(Long dsId, String sql);
}
