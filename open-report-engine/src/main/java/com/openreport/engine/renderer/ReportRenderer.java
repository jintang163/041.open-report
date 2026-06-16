package com.openreport.engine.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.model.ChartConfig;
import com.openreport.engine.model.ReportCell;
import com.openreport.engine.model.ReportTemplate;
import com.openreport.engine.model.RenderResult;
import com.openreport.engine.parser.CellExpressionParser;
import com.openreport.engine.calcite.CalciteQueryExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ReportRenderer {

    @Autowired
    private CellExpressionParser cellExpressionParser;

    @Autowired
    private CalciteQueryExecutor calciteQueryExecutor;

    @Autowired
    private HtmlTableRenderer htmlTableRenderer;

    @Autowired
    private ChartOptionGenerator chartOptionGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RenderResult render(ReportTemplate template, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        RenderResult result = new RenderResult();
        result.setReportId(template.getTemplateId());

        Map<String, List<Map<String, Object>>> dataSets = executeDataSets(template, parameters);
        result.setDataSets(dataSets);

        List<List<ReportCell>> cellMatrix = buildCellMatrix(template);
        fillCellValues(cellMatrix, dataSets, parameters);
        result.setCellMatrix(cellMatrix);

        String htmlContent = htmlTableRenderer.render(cellMatrix);
        result.setHtmlContent(htmlContent);

        Map<String, Object> chartOptions = generateChartOptions(template, dataSets);
        result.setChartOptions(chartOptions);

        Map<String, Object> meta = new HashMap<>();
        meta.put("templateName", template.getTemplateName());
        meta.put("templateCode", template.getTemplateCode());
        result.setMeta(meta);

        result.setCostTime(System.currentTimeMillis() - startTime);
        return result;
    }

    private Map<String, List<Map<String, Object>>> executeDataSets(ReportTemplate template, Map<String, Object> parameters) {
        Map<String, List<Map<String, Object>>> dataSets = new LinkedHashMap<>();
        if (template.getDataSets() == null || template.getDataSets().isEmpty()) {
            return dataSets;
        }

        for (com.openreport.engine.model.DataSetBind dataSetBind : template.getDataSets()) {
            Map<String, Object> queryParams = new HashMap<>();
            if (dataSetBind.getParameters() != null) {
                queryParams.putAll(dataSetBind.getParameters());
            }
            if (parameters != null) {
                queryParams.putAll(parameters);
            }

            List<Map<String, Object>> data = calciteQueryExecutor.executeQuery(
                    dataSetBind.getDataSourceId(),
                    dataSetBind.getSql(),
                    queryParams
            );
            dataSets.put(dataSetBind.getDataSetId(), data);
        }
        return dataSets;
    }

    private List<List<ReportCell>> buildCellMatrix(ReportTemplate template) {
        if (template.getCells() == null || template.getCells().isEmpty()) {
            return new ArrayList<>();
        }

        int maxRow = 0;
        int maxCol = 0;
        for (ReportCell cell : template.getCells()) {
            maxRow = Math.max(maxRow, cell.getRowIndex() + cell.getRowSpan());
            maxCol = Math.max(maxCol, cell.getColIndex() + cell.getColSpan());
        }

        List<List<ReportCell>> matrix = new ArrayList<>(maxRow);
        for (int i = 0; i < maxRow; i++) {
            List<ReportCell> row = new ArrayList<>(maxCol);
            for (int j = 0; j < maxCol; j++) {
                row.add(null);
            }
            matrix.add(row);
        }

        for (ReportCell cell : template.getCells()) {
            matrix.get(cell.getRowIndex()).set(cell.getColIndex(), cell);
        }

        return matrix;
    }

    private void fillCellValues(List<List<ReportCell>> cellMatrix,
                                 Map<String, List<Map<String, Object>>> dataSets,
                                 Map<String, Object> parameters) {
        for (List<ReportCell> row : cellMatrix) {
            for (ReportCell cell : row) {
                if (cell != null && cell.getExpression() != null && !cell.getExpression().isEmpty()) {
                    try {
                        Object value = cellExpressionParser.parseObjectContent(
                                cell.getExpression(),
                                dataSets,
                                0,
                                parameters
                        );
                        cell.setValue(value);
                    } catch (Exception e) {
                        log.warn("Failed to evaluate expression: {}", cell.getExpression(), e);
                        cell.setValue(null);
                    }
                }
            }
        }
    }

    private Map<String, Object> generateChartOptions(ReportTemplate template,
                                                      Map<String, List<Map<String, Object>>> dataSets) {
        Map<String, Object> chartOptions = new LinkedHashMap<>();
        if (template.getCharts() == null || template.getCharts().isEmpty()) {
            return chartOptions;
        }

        for (Map.Entry<String, ChartConfig> entry : template.getCharts().entrySet()) {
            ChartConfig chartConfig = entry.getValue();
            List<Map<String, Object>> data = dataSets.get(chartConfig.getDataSetId());
            if (data != null) {
                Map<String, Object> option = chartOptionGenerator.generateOption(chartConfig, data);
                chartOptions.put(entry.getKey(), option);
            }
        }
        return chartOptions;
    }
}
