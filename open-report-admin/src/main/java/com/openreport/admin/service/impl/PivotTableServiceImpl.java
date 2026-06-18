package com.openreport.admin.service.impl;

import com.openreport.admin.entity.DataSet;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.PivotTableService;
import com.openreport.engine.pivot.PivotTableEngine;
import com.openreport.engine.pivot.model.PivotTableConfig;
import com.openreport.engine.pivot.model.PivotTableResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交叉报表服务实现
 */
@Service
public class PivotTableServiceImpl implements PivotTableService {

    @Autowired
    private DataSetService dataSetService;

    private final PivotTableEngine pivotTableEngine = new PivotTableEngine();

    @Override
    public PivotTableResult executePivotQuery(PivotTableConfig config, Map<String, Object> params) {
        if (config == null || config.getDataSetId() == null) {
            throw new IllegalArgumentException("交叉报表配置或数据集ID不能为空");
        }

        DataSet dataSet = dataSetService.getById(config.getDataSetId());
        if (dataSet == null) {
            throw new IllegalArgumentException("数据集不存在");
        }

        String baseSql = dataSet.getSqlText();
        if (StringUtils.isBlank(baseSql)) {
            throw new IllegalArgumentException("数据集SQL不能为空");
        }

        String groupBySql = pivotTableEngine.buildGroupBySql(baseSql, config);

        List<Map<String, Object>> aggregatedData = dataSetService.executeCustomSql(
                config.getDataSetId(), groupBySql, params);

        return pivotTableEngine.pivotResultSet(aggregatedData, config);
    }

    @Override
    public Map<String, Object> previewPivotData(PivotTableConfig config, Map<String, Object> params, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (config == null || config.getDataSetId() == null) {
                result.put("success", false);
                result.put("message", "交叉报表配置或数据集ID不能为空");
                return result;
            }

            Map<String, Object> rawResult = dataSetService.previewData(config.getDataSetId(), params, limit);
            if (!Boolean.TRUE.equals(rawResult.get("success"))) {
                return rawResult;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawData = (List<Map<String, Object>>) rawResult.get("rows");

            PivotTableResult pivotResult = pivotTableEngine.pivotResultSet(rawData, config);

            result.put("success", true);
            result.put("rawData", rawResult);
            result.put("pivotData", pivotResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "交叉报表预览失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public String generatePivotSql(PivotTableConfig config) {
        if (config == null || config.getDataSetId() == null) {
            throw new IllegalArgumentException("交叉报表配置或数据集ID不能为空");
        }

        DataSet dataSet = dataSetService.getById(config.getDataSetId());
        if (dataSet == null) {
            throw new IllegalArgumentException("数据集不存在");
        }

        String baseSql = dataSet.getSqlText();
        if (StringUtils.isBlank(baseSql)) {
            throw new IllegalArgumentException("数据集SQL不能为空");
        }

        return pivotTableEngine.buildGroupBySql(baseSql, config);
    }
}
