package com.openreport.engine.service;

import com.openreport.engine.datasource.DataSourceConnectionTester;
import com.openreport.engine.model.ReportTemplate;
import com.openreport.engine.model.RenderResult;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ReportEngineService {

    RenderResult renderReport(ReportTemplate template, Map<String, Object> parameters);

    RenderResult renderReport(String templateJson, Map<String, Object> parameters);

    byte[] exportExcel(ReportTemplate template, Map<String, Object> parameters);

    byte[] exportExcel(String templateJson, Map<String, Object> parameters);

    byte[] exportExcelByTemplate(InputStream templateInputStream, Map<String, Object> data);

    byte[] exportPdf(ReportTemplate template, Map<String, Object> parameters);

    byte[] exportPdf(String templateJson, Map<String, Object> parameters);

    List<Map<String, Object>> executeQuery(String dataSourceId, String sql, Map<String, Object> parameters);

    Map<String, Object> executeSingleRow(String dataSourceId, String sql, Map<String, Object> parameters);

    Object executeSingleValue(String dataSourceId, String sql, Map<String, Object> parameters);

    void registerDataSource(String dataSourceId, String dataSourceName,
                             String driverClassName, String url,
                             String username, String password,
                             Map<String, Object> properties);

    void unregisterDataSource(String dataSourceId);

    DataSourceConnectionTester.TestResult testDataSource(String driverClassName, String url,
                                                          String username, String password,
                                                          Map<String, Object> properties);
}
