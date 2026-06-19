package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.DataLineage;

import java.util.List;
import java.util.Map;

public interface DataLineageService extends IService<DataLineage> {

    List<DataLineage> getLineageByReport(Long reportId);

    List<DataLineage> getLineageByReportField(Long reportId, String reportField);

    List<DataLineage> getLineageByDataSet(Long dataSetId);

    List<DataLineage> getLineageByDatasource(Long datasourceId);

    List<DataLineage> getLineageByTable(Long datasourceId, String tableName);

    List<DataLineage> getLineageByTableColumn(Long datasourceId, String tableName, String columnName);

    List<DataLineage> getAffectedReports(Long datasourceId, String tableName, String columnName);

    List<DataLineage> getAffectedDataSets(Long datasourceId, String tableName, String columnName);

    Map<String, Object> refreshLineageForReport(Long reportId);

    Map<String, Object> refreshLineageForDataSet(Long dataSetId);

    Map<String, Object> parseSqlAndExtractLineage(Long dataSetId);

    Map<String, Object> analyzeImpact(Long datasourceId, String tableName, String columnName);

    Map<String, Object> getLineageTree(Long reportId);

    Map<String, Object> getLineageTrace(Long reportId, String reportField);

    boolean deleteLineageByReport(Long reportId);

    boolean deleteLineageByDataSet(Long dataSetId);
}
