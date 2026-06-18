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
    public CalciteQueryExecutor.QueryPageResult executeQueryPage(String dataSourceId, String sql, Map<String, Object> parameters,
                                                                  int pageNum, int pageSize) {
        log.info("Executing page query on dataSource: {}, pageNum: {}, pageSize: {}", dataSourceId, pageNum, pageSize);
        return calciteQueryExecutor.executeQueryPage(dataSourceId, sql, parameters, pageNum, pageSize);
    }

    @Override
    public void executeQueryStreaming(String dataSourceId, String sql, Map<String, Object> parameters,
                                       CalciteQueryExecutor.RowCallback rowCallback) {
        log.info("Executing streaming query on dataSource: {}", dataSourceId);
        calciteQueryExecutor.executeQueryStreaming(dataSourceId, sql, parameters, rowCallback);
    }

    @Override
    public void executeQueryBatch(String dataSourceId, String sql, Map<String, Object> parameters,
                                   int batchSize, CalciteQueryExecutor.BatchCallback batchCallback) {
        log.info("Executing batch query on dataSource: {}, batchSize: {}", dataSourceId, batchSize);
        calciteQueryExecutor.executeQueryBatch(dataSourceId, sql, parameters, batchSize, batchCallback);
    }

    @Override
    public CalciteQueryExecutor.QueryPageResult queryDataSetPage(String templateJson, String dataSetId,
                                                                  Map<String, Object> parameters,
                                                                  int pageNum, int pageSize) {
        log.info("Query data set page, dataSetId: {}, pageNum: {}, pageSize: {}", dataSetId, pageNum, pageSize);
        ReportTemplate template = reportTemplateParser.parse(templateJson);
        if (template.getDataSets() == null || template.getDataSets().isEmpty()) {
            CalciteQueryExecutor.QueryPageResult result = new CalciteQueryExecutor.QueryPageResult();
            result.setTotal(0);
            result.setPageNum(pageNum);
            result.setPageSize(pageSize);
            result.setList(new java.util.ArrayList<>());
            return result;
        }

        com.openreport.engine.model.DataSetBind dataSetBind = null;
        for (com.openreport.engine.model.DataSetBind bind : template.getDataSets()) {
            if (bind.getDataSetId().equals(dataSetId)) {
                dataSetBind = bind;
                break;
            }
        }
        if (dataSetBind == null) {
            // 返回第一个数据集
            dataSetBind = template.getDataSets().get(0);
        }

        Map<String, Object> queryParams = new java.util.HashMap<>();
        if (dataSetBind.getParameters() != null) {
            queryParams.putAll(dataSetBind.getParameters());
        }
        if (parameters != null) {
            queryParams.putAll(parameters);
        }

        return calciteQueryExecutor.executeQueryPage(
                dataSetBind.getDataSourceId(),
                dataSetBind.getSql(),
                queryParams,
                pageNum,
                pageSize
        );
    }

    @Override
    public byte[] exportReportStreaming(String templateJson, Map<String, Object> parameters, String outputType) {
        log.info("Export report streaming, outputType: {}", outputType);
        ReportTemplate template = reportTemplateParser.parse(templateJson);

        if ("PDF".equalsIgnoreCase(outputType)) {
            return pdfExporter.exportFromRenderResult(reportRenderer.render(template, parameters));
        }

        if (template.getDataSets() == null || template.getDataSets().isEmpty()) {
            return excelExporter.exportFromRenderResult(reportRenderer.render(template, parameters));
        }

        com.openreport.engine.model.DataSetBind dataSetBind = template.getDataSets().get(0);
        Map<String, Object> queryParams = new java.util.HashMap<>();
        if (dataSetBind.getParameters() != null) {
            queryParams.putAll(dataSetBind.getParameters());
        }
        if (parameters != null) {
            queryParams.putAll(parameters);
        }

        final String dataSourceId = dataSetBind.getDataSourceId();
        final String sql = dataSetBind.getSql();
        final String sheetName = dataSetBind.getDataSetName() != null
                ? dataSetBind.getDataSetName() : "Data";

        final java.util.concurrent.atomic.AtomicReference<byte[]> resultRef = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicBoolean firstBatch = new java.util.concurrent.atomic.AtomicBoolean(true);
        final java.util.concurrent.atomic.AtomicReference<ExcelExporter.StreamingWriter> writerRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        byte[] result = excelExporter.exportDataSetStreaming(
                sheetName,
                0,
                null,
                writer -> {
                    writerRef.set(writer);
                    calciteQueryExecutor.executeQueryBatch(dataSourceId, sql, queryParams, 1000,
                            (batch, batchIndex, totalCount) -> {
                                if (batch != null && !batch.isEmpty()) {
                                    writer.writeBatch(batch);
                                }
                            });
                }
        );

        return result;
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
