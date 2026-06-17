package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.mapper.DataSetMapper;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.admin.service.DataSetService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataSetServiceImpl extends ServiceImpl<DataSetMapper, DataSet> implements DataSetService {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Override
    public Page<DataSet> pageList(Integer pageNum, Integer pageSize, String setName, Long dsId) {
        Page<DataSet> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DataSet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(setName)) {
            wrapper.like(DataSet::getSetName, setName);
        }
        if (dsId != null) {
            wrapper.eq(DataSet::getDsId, dsId);
        }
        wrapper.orderByDesc(DataSet::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<DataSet> listByDsId(Long dsId) {
        LambdaQueryWrapper<DataSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSet::getDsId, dsId);
        wrapper.eq(DataSet::getStatus, 1);
        wrapper.orderByAsc(DataSet::getSetName);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> previewData(Long dataSetId, Map<String, Object> params, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        DataSet dataSet = getById(dataSetId);
        if (dataSet == null) {
            result.put("success", false);
            result.put("message", "数据集不存在");
            return result;
        }
        DataSourceConfig dsConfig = dataSourceConfigService.getById(dataSet.getDsId());
        if (dsConfig == null) {
            result.put("success", false);
            result.put("message", "数据源不存在");
            return result;
        }
        String sql = dataSet.getSqlText();
        if (StringUtils.isBlank(sql)) {
            result.put("success", false);
            result.put("message", "SQL不能为空");
            return result;
        }
        if (limit != null && limit > 0) {
            sql = sql + " LIMIT " + limit;
        }
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName(dsConfig.getDriverClass());
            connection = DriverManager.getConnection(dsConfig.getJdbcUrl(), dsConfig.getUsername(), dsConfig.getPassword());
            ps = connection.prepareStatement(sql);
            if (params != null && !params.isEmpty()) {
                List<Map<String, Object>> paramList = parseParamsFromSql(sql);
                int index = 1;
                for (Map<String, Object> param : paramList) {
                    String paramName = param.get("name").toString();
                    if (params.containsKey(paramName)) {
                        ps.setObject(index++, params.get(paramName));
                    }
                }
            }
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Map<String, Object>> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", metaData.getColumnLabel(i));
                column.put("type", metaData.getColumnTypeName(i));
                columns.add(column);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            result.put("success", true);
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("total", rows.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行SQL失败: " + e.getMessage());
        } finally {
            closeResources(rs, ps, connection);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> previewDataList(Long dataSetId, Map<String, Object> params) {
        Map<String, Object> result = previewData(dataSetId, params, 10000);
        if (Boolean.TRUE.equals(result.get("success"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
            return rows != null ? rows : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> parseSql(String sqlText) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> params = parseParamsFromSql(sqlText);
        result.put("params", params);
        result.put("paramCount", params.size());
        return result;
    }

    private List<Map<String, Object>> parseParamsFromSql(String sql) {
        List<Map<String, Object>> params = new ArrayList<>();
        Pattern pattern = Pattern.compile("#\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
        Matcher matcher = pattern.matcher(sql);
        Set<String> paramNames = new LinkedHashSet<>();
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        int index = 1;
        for (String paramName : paramNames) {
            Map<String, Object> param = new HashMap<>();
            param.put("name", paramName);
            param.put("type", "String");
            param.put("required", true);
            param.put("index", index++);
            params.add(param);
        }
        return params;
    }

    private void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
