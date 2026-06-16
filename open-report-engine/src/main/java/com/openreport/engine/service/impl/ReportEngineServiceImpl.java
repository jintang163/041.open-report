package com.openreport.engine.service.impl;

import com.openreport.engine.calcite.CalciteQueryExecutor;
import com.openreport.engine.datasource.DataSourceConnectionTester;
import com.openreport.engine.datasource.DynamicDataSourceManager;
import com.openreport.engine.export.ExcelExporter;
import com.openreport.engine.export.PdfExporter;
import com.openreport.engine.model.ReportTemplate;
import com.openreport.engine.model.RenderResult;
import com.openreport.engine.parser.ReportTemplateParser;
import com.openreport.engine.renderer.ReportRenderer;
import com.openreport.engine.service.ReportEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportEngineServiceImpl implements ReportEngineService {

    @Autowired
    private ReportRenderer reportRenderer;

    @Autowired
    private ReportTemplateParser reportTemplateParser;

    @Autowired
    private CalciteQueryExecutor calciteQueryExecutor;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired
    private DataSourceConnectionTester dataSourceConnectionTester;

    @Autowired
    private ExcelExporter excelExporter;

    @Autowired
    private PdfExporter pdfExporter;

    @Override
    public RenderResult renderReport(ReportTemplate template, Map<String, Object> parameters) {
        log.info("Rendering report: {}", template.getTemplateId());
        return reportRenderer.render(template, parameters);
    }

    @Override
    public RenderResult renderReport(String templateJson, Map<String, Object> parameters) {
        ReportTemplate template = reportTemplateParser.parse(templateJson);
        return renderReport(template, parameters);
    }

    @Override
    public byte[] exportExcel(ReportTemplate template, Map<String, Object> parameters) {
        log.info("Exporting report to Excel: {}", template.getTemplateId());
        RenderResult renderResult = reportRenderer.render(template, parameters);
        return excelExporter.exportFromRenderResult(renderResult);
    }

    @Override
    public byte[] exportExcel(String templateJson, Map<String, Object> parameters) {
        ReportTemplate template = reportTemplateParser.parse(templateJson);
        return exportExcel(template, parameters);
    }

    @Override
    public byte[] exportExcelByTemplate(InputStream templateInputStream, Map<String, Object> data) {
        return excelExporter.exportWithTemplate(templateInputStream, data);
    }

    @Override
    public byte[] exportPdf(ReportTemplate template, Map<String, Object> parameters) {
        log.info("Exporting report to PDF: {}", template.getTemplateId());
        RenderResult renderResult = reportRenderer.render(template, parameters);
        return pdfExporter.exportFromRenderResult(renderResult);
    }

    @Override
    public byte[] exportPdf(String templateJson, Map<String, Object> parameters) {
        ReportTemplate template = reportTemplateParser.parse(templateJson);
        return exportPdf(template, parameters);
    }

    @Override
    public List<Map<String, Object>> executeQuery(String dataSourceId, String sql, Map<String, Object> parameters) {
        log.info("Executing query on dataSource: {}", dataSourceId);
        return calciteQueryExecutor.executeQuery(dataSourceId, sql, parameters);
    }

    @Override
    public Map<String, Object> executeSingleRow(String dataSourceId, String sql, Map<String, Object> parameters) {
        return calciteQueryExecutor.executeSingleRow(dataSourceId, sql, parameters);
    }

    @Override
    public Object executeSingleValue(String dataSourceId, String sql, Map<String, Object> parameters) {
        return calciteQueryExecutor.executeSingleValue(dataSourceId, sql, parameters);
    }

    @Override
    public void registerDataSource(String dataSourceId, String dataSourceName,
                                    String driverClassName, String url,
                                    String username, String password,
                                    Map<String, Object> properties) {
        log.info("Registering data source: {}", dataSourceId);
        dynamicDataSourceManager.createDataSource(dataSourceId, dataSourceName,
                driverClassName, url, username, password, properties);
    }

    @Override
    public void unregisterDataSource(String dataSourceId) {
        log.info("Unregistering data source: {}", dataSourceId);
        dynamicDataSourceManager.removeDataSource(dataSourceId);
    }

    @Override
    public DataSourceConnectionTester.TestResult testDataSource(String driverClassName, String url,
                                                                  String username, String password,
                                                                  Map<String, Object> properties) {
        return dataSourceConnectionTester.testConnection(driverClassName, url, username, password, properties);
    }
}
