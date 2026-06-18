package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.mapper.DataSetMapper;
import com.openreport.admin.service.DataSecurityService;
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

    @Autowired
    private DataSecurityService dataSecurityService;

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
        sql = dataSecurityService.applyRowSecurity(sql, dataSetId);
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
                List<Map<String, Object>> paramList = parseParamsFromSql(dataSet.getSqlText());
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
            String tableName = extractMainTableName(dataSet.getSqlText());
            rows = dataSecurityService.filterHiddenFields(rows, tableName);
            rows = dataSecurityService.applyFieldMasking(rows, tableName);
            columns = dataSecurityService.filterHiddenColumns(columns, tableName);
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
    public Map<String, Object> pagePreviewData(Long dataSetId, Map<String, Object> params, int pageNum, int pageSize) {
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

        sql = dataSecurityService.applyRowSecurity(sql, dataSetId);

        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t_cnt";
        String pageSql = sql + " LIMIT " + pageSize + " OFFSET " + ((pageNum - 1) * pageSize);

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName(dsConfig.getDriverClass());
            connection = DriverManager.getConnection(dsConfig.getJdbcUrl(), dsConfig.getUsername(), dsConfig.getPassword());

            long total = 0;
            try {
                ps = connection.prepareStatement(countSql);
                setParams(ps, dataSet.getSqlText(), params);
                rs = ps.executeQuery();
                if (rs.next()) {
                    total = rs.getLong(1);
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                log.warn("Count query failed, using -1 as total", e);
                total = -1;
            }

            ps = connection.prepareStatement(pageSql);
            setParams(ps, dataSet.getSqlText(), params);
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

            String tableName = extractMainTableName(dataSet.getSqlText());
            rows = dataSecurityService.filterHiddenFields(rows, tableName);
            rows = dataSecurityService.applyFieldMasking(rows, tableName);
            columns = dataSecurityService.filterHiddenColumns(columns, tableName);

            result.put("success", true);
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("hasMore", total == -1 ? rows.size() >= pageSize : (long) pageNum * pageSize < total);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行SQL失败: " + e.getMessage());
        } finally {
            closeResources(rs, ps, connection);
        }
        return result;
    }

    private void setParams(PreparedStatement ps, String sql, Map<String, Object> params) throws SQLException {
        if (params == null || params.isEmpty()) {
            return;
        }
        List<Map<String, Object>> paramList = parseParamsFromSql(sql);
        int index = 1;
        for (Map<String, Object> param : paramList) {
            String paramName = param.get("name").toString();
            if (params.containsKey(paramName)) {
                ps.setObject(index++, params.get(paramName));
            }
        }
    }

    @Override
    public long countData(Long dataSetId, Map<String, Object> params) {
        DataSet dataSet = getById(dataSetId);
        if (dataSet == null) {
            return -1L;
        }
        DataSourceConfig dsConfig = dataSourceConfigService.getById(dataSet.getDsId());
        if (dsConfig == null) {
            return -1L;
        }
        String sql = dataSet.getSqlText();
        if (StringUtils.isBlank(sql)) {
            return -1L;
        }
        sql = dataSecurityService.applyRowSecurity(sql, dataSetId);
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t_cnt";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName(dsConfig.getDriverClass());
            connection = DriverManager.getConnection(dsConfig.getJdbcUrl(), dsConfig.getUsername(), dsConfig.getPassword());
            ps = connection.prepareStatement(countSql);
            setParams(ps, sql, params);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.warn("Count data failed for dataSetId: {}", dataSetId, e);
        } finally {
            closeResources(rs, ps, connection);
        }
        return -1L;
    }

    @Override
    public Map<String, Object> previewDataWithCount(Long dataSetId, Map<String, Object> params, Integer limit) {
        Map<String, Object> result = previewData(dataSetId, params, limit);
        long total = countData(dataSetId, params);
        result.put("total", total);
        return result;
    }

    @Override
    public void streamBatchData(Long dataSetId, Map<String, Object> params, int batchSize,
                                java.util.function.Consumer<java.util.List<Map<String, Object>>> batchCallback) {
        DataSet dataSet = getById(dataSetId);
        if (dataSet == null || batchCallback == null) {
            return;
        }
        DataSourceConfig dsConfig = dataSourceConfigService.getById(dataSet.getDsId());
        if (dsConfig == null) {
            return;
        }
        String sql = dataSet.getSqlText();
        if (StringUtils.isBlank(sql)) {
            return;
        }
        sql = dataSecurityService.applyRowSecurity(sql, dataSetId);
        String tableName = extractMainTableName(dataSet.getSqlText());
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName(dsConfig.getDriverClass());
            connection = DriverManager.getConnection(dsConfig.getJdbcUrl(), dsConfig.getUsername(), dsConfig.getPassword());
            ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            try {
                ps.setFetchSize(Integer.MIN_VALUE);
            } catch (Exception ignored) {
            }
            setParams(ps, dataSet.getSqlText(), params);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            String[] columnLabels = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                columnLabels[i - 1] = metaData.getColumnLabel(i);
            }
            java.util.List<Map<String, Object>> batch = new ArrayList<>(batchSize);
            int count = 0;
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < columnCount; i++) {
                    row.put(columnLabels[i], rs.getObject(i + 1));
                }
                batch.add(row);
                count++;
                if (count >= batchSize) {
                    List<Map<String, Object>> filteredBatch = dataSecurityService.filterHiddenFields(batch, tableName);
                    filteredBatch = dataSecurityService.applyFieldMasking(filteredBatch, tableName);
                    batchCallback.accept(new ArrayList<>(filteredBatch));
                    batch.clear();
                    count = 0;
                }
            }
            if (!batch.isEmpty()) {
                List<Map<String, Object>> filteredBatch = dataSecurityService.filterHiddenFields(batch, tableName);
                filteredBatch = dataSecurityService.applyFieldMasking(filteredBatch, tableName);
                batchCallback.accept(filteredBatch);
            }
        } catch (Exception e) {
            log.error("Stream batch data failed for dataSetId: {}", dataSetId, e);
        } finally {
            closeResources(rs, ps, connection);
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSetServiceImpl.class);

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

    private String extractMainTableName(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "*";
        }
        Pattern tablePattern = Pattern.compile("(?i)\\bFROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = tablePattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "*";
    }
}
