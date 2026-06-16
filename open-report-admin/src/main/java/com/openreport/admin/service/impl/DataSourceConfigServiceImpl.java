package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.mapper.DataSourceConfigMapper;
import com.openreport.admin.service.DataSourceConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> implements DataSourceConfigService {

    @Override
    public Page<DataSourceConfig> pageList(Integer pageNum, Integer pageSize, String dsName, String dsType) {
        Page<DataSourceConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dsName)) {
            wrapper.like(DataSourceConfig::getDsName, dsName);
        }
        if (StringUtils.isNotBlank(dsType)) {
            wrapper.eq(DataSourceConfig::getDsType, dsType);
        }
        wrapper.orderByDesc(DataSourceConfig::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<DataSourceConfig> listAll() {
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceConfig::getStatus, 1);
        wrapper.orderByAsc(DataSourceConfig::getDsName);
        return list(wrapper);
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        Connection connection = null;
        try {
            Class.forName(config.getDriverClass());
            connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            log.error("测试数据库连接失败", e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public Map<String, Object> getConnectionInfo(Long id) {
        DataSourceConfig config = getById(id);
        Map<String, Object> result = new HashMap<>();
        if (config != null) {
            result.put("driverClass", config.getDriverClass());
            result.put("jdbcUrl", config.getJdbcUrl());
            result.put("username", config.getUsername());
            result.put("password", config.getPassword());
            result.put("schemaName", config.getSchemaName());
        }
        return result;
    }
}
