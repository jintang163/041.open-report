package com.openreport.admin.service;

import com.openreport.engine.pivot.model.PivotTableConfig;
import com.openreport.engine.pivot.model.PivotTableResult;

import java.util.Map;

/**
 * 交叉报表服务接口
 */
public interface PivotTableService {

    PivotTableResult executePivotQuery(PivotTableConfig config, Map<String, Object> params);

    Map<String, Object> previewPivotData(PivotTableConfig config, Map<String, Object> params, Integer limit);

    String generatePivotSql(PivotTableConfig config);
}
