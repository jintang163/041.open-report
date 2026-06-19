package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.DataLineage;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.mapper.DataLineageMapper;
import com.openreport.admin.service.DataLineageService;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.admin.utils.SqlParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataLineageServiceImpl extends ServiceImpl<DataLineageMapper, DataLineage>
        implements DataLineageService {

    private static final Logger logger = LoggerFactory.getLogger(DataLineageServiceImpl.class);

    @Autowired
    private DataLineageMapper lineageMapper;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Override
    public List<DataLineage> getLineageByReport(Long reportId) {
        return lineageMapper.selectByReportId(reportId);
    }

    @Override
    public List<DataLineage> getLineageByReportField(Long reportId, String reportField) {
        return Collections.singletonList(lineageMapper.selectByReportField(reportId, reportField));
    }

    @Override
    public List<DataLineage> getLineageByDataSet(Long dataSetId) {
        return lineageMapper.selectByDataSetId(dataSetId);
    }

    @Override
    public List<DataLineage> getLineageByDatasource(Long datasourceId) {
        return lineageMapper.selectByDatasourceId(datasourceId);
    }

    @Override
    public List<DataLineage> getLineageByTable(Long datasourceId, String tableName) {
        return lineageMapper.selectByTable(datasourceId, tableName);
    }

    @Override
    public List<DataLineage> getLineageByTableColumn(Long datasourceId, String tableName, String columnName) {
        return lineageMapper.selectByTableColumn(datasourceId, tableName, columnName);
    }

    @Override
    public List<DataLineage> getAffectedReports(Long datasourceId, String tableName, String columnName) {
        return lineageMapper.selectAffectedReports(datasourceId, tableName, columnName);
    }

    @Override
    public List<DataLineage> getAffectedDataSets(Long datasourceId, String tableName, String columnName) {
        return lineageMapper.selectAffectedDataSets(datasourceId, tableName, columnName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshLineageForReport(Long reportId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);

        ReportTemplate template = reportTemplateService.getById(reportId);
        if (template == null) {
            result.put("message", "报表不存在");
            return result;
        }

        try {
            lineageMapper.deleteByReportId(reportId);
            int lineageCount = buildLineageForTemplate(template);

            result.put("success", true);
            result.put("reportId", reportId);
            result.put("reportName", template.getTemplateName());
            result.put("lineageCount", lineageCount);
            result.put("message", "血缘关系刷新成功，共生成 " + lineageCount + " 条血缘记录");

            logger.info("刷新报表血缘成功: reportId={}, lineageCount={}", reportId, lineageCount);
        } catch (Exception e) {
            logger.error("刷新报表血缘失败: reportId={}", reportId, e);
            result.put("message", "血缘关系刷新失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshLineageForDataSet(Long dataSetId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);

        DataSet dataSet = dataSetService.getById(dataSetId);
        if (dataSet == null) {
            result.put("message", "数据集不存在");
            return result;
        }

        try {
            lineageMapper.deleteByDataSetId(dataSetId);

            List<ReportTemplate> templates = findReportsUsingDataSet(dataSetId);
            int totalLineage = 0;

            for (ReportTemplate template : templates) {
                lineageMapper.deleteByReportId(template.getId());
                totalLineage += buildLineageForTemplate(template);
            }

            result.put("success", true);
            result.put("dataSetId", dataSetId);
            result.put("affectedReportCount", templates.size());
            result.put("lineageCount", totalLineage);
            result.put("message", "数据集血缘刷新成功，影响 " + templates.size() + " 个报表，共 " + totalLineage + " 条血缘记录");

            logger.info("刷新数据集血缘成功: dataSetId={}, affectedReports={}, lineageCount={}",
                    dataSetId, templates.size(), totalLineage);
        } catch (Exception e) {
            logger.error("刷新数据集血缘失败: dataSetId={}", dataSetId, e);
            result.put("message", "数据集血缘刷新失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> parseSqlAndExtractLineage(Long dataSetId) {
        Map<String, Object> result = new LinkedHashMap<>();

        DataSet dataSet = dataSetService.getById(dataSetId);
        if (dataSet == null) {
            result.put("success", false);
            result.put("message", "数据集不存在");
            return result;
        }

        try {
            String sqlText = dataSet.getSqlText();
            SqlParseUtils.ParseResult parseResult = SqlParseUtils.parseSql(sqlText);

            DataSourceConfig dataSource = dataSourceConfigService.getById(dataSet.getDsId());

            result.put("success", true);
            result.put("dataSetId", dataSetId);
            result.put("dataSetName", dataSet.getSetName());
            result.put("sqlText", sqlText);
            result.put("tables", parseResult.getTables());
            result.put("columns", parseResult.getColumns());
            result.put("selectColumns", parseResult.getSelectColumns());
            result.put("whereColumns", parseResult.getWhereColumns());
            result.put("aggregations", parseResult.getAggregations());
            result.put("hasAggregation", parseResult.hasAggregation());
            result.put("tableAliases", parseResult.getTableAliases());
            result.put("mainTable", parseResult.getMainTable());

            if (dataSource != null) {
                result.put("datasourceId", dataSource.getId());
                result.put("datasourceName", dataSource.getDsName());
                result.put("datasourceType", dataSource.getDsType());
                result.put("databaseName", SqlParseUtils.extractDatabaseNameFromJdbcUrl(dataSource.getJdbcUrl()));
                result.put("schemaName", dataSource.getSchemaName());
            }

            logger.info("SQL解析成功: dataSetId={}, tables={}, columns={}",
                    dataSetId, parseResult.getTables(), parseResult.getColumns());
        } catch (Exception e) {
            logger.error("SQL解析失败: dataSetId={}", dataSetId, e);
            result.put("success", false);
            result.put("message", "SQL解析失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> analyzeImpact(Long datasourceId, String tableName, String columnName) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            List<DataLineage> affectedReports = getAffectedReports(datasourceId, tableName, columnName);
            List<DataLineage> affectedDataSets = getAffectedDataSets(datasourceId, tableName, columnName);
            List<DataLineage> allLineage = getLineageByTableColumn(datasourceId, tableName, columnName);

            Set<Long> reportIds = new LinkedHashSet<>();
            Set<Long> dataSetIds = new LinkedHashSet<>();
            Set<String> reportFields = new LinkedHashSet<>();
            Map<String, List<DataLineage>> lineageByReport = new LinkedHashMap<>();

            for (DataLineage lineage : allLineage) {
                if (lineage.getReportId() != null) {
                    reportIds.add(lineage.getReportId());
                    reportFields.add(lineage.getReportName() + "." + lineage.getReportField());
                }
                if (lineage.getDataSetId() != null) {
                    dataSetIds.add(lineage.getDataSetId());
                }
                lineageByReport
                        .computeIfAbsent(lineage.getReportName(), k -> new ArrayList<>())
                        .add(lineage);
            }

            result.put("success", true);
            result.put("datasourceId", datasourceId);
            result.put("tableName", tableName);
            result.put("columnName", columnName);
            result.put("affectedReportCount", affectedReports.size());
            result.put("affectedDataSetCount", affectedDataSets.size());
            result.put("affectedFieldCount", reportFields.size());
            result.put("affectedReports", affectedReports);
            result.put("affectedDataSets", affectedDataSets);
            result.put("affectedFields", new ArrayList<>(reportFields));
            result.put("lineageByReport", lineageByReport);
            result.put("allLineage", allLineage);

            DataSourceConfig dataSource = dataSourceConfigService.getById(datasourceId);
            if (dataSource != null) {
                result.put("datasourceName", dataSource.getDsName());
                result.put("datasourceType", dataSource.getDsType());
            }

            logger.info("影响分析完成: datasourceId={}, table={}, column={}, affectedReports={}",
                    datasourceId, tableName, columnName, affectedReports.size());
        } catch (Exception e) {
            logger.error("影响分析失败: datasourceId={}, table={}, column={}",
                    datasourceId, tableName, columnName, e);
            result.put("success", false);
            result.put("message", "影响分析失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getLineageTree(Long reportId) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            List<DataLineage> lineages = getLineageByReport(reportId);
            ReportTemplate template = reportTemplateService.getById(reportId);

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("id", "report_" + reportId);
            root.put("name", template != null ? template.getTemplateName() : "报表_" + reportId);
            root.put("type", "report");
            root.put("children", new ArrayList<>());

            Map<Long, Map<String, Object>> dataSetNodes = new LinkedHashMap<>();
            Map<String, Map<String, Object>> tableNodes = new LinkedHashMap<>();

            for (DataLineage lineage : lineages) {
                Map<String, Object> fieldNode = new LinkedHashMap<>();
                fieldNode.put("id", "field_" + lineage.getReportField());
                fieldNode.put("name", lineage.getReportFieldTitle() != null ? lineage.getReportFieldTitle() : lineage.getReportField());
                fieldNode.put("type", "reportField");
                fieldNode.put("dataSetField", lineage.getDataSetField());
                fieldNode.put("expression", lineage.getExpression());
                fieldNode.put("lineageType", lineage.getLineageType());
                fieldNode.put("tableName", lineage.getTableName());
                fieldNode.put("columnName", lineage.getColumnName());

                Long dataSetId = lineage.getDataSetId();
                Map<String, Object> dataSetNode = dataSetNodes.get(dataSetId);
                if (dataSetNode == null) {
                    dataSetNode = new LinkedHashMap<>();
                    dataSetNode.put("id", "dataset_" + dataSetId);
                    dataSetNode.put("name", lineage.getDataSetName());
                    dataSetNode.put("type", "dataSet");
                    dataSetNode.put("datasourceId", lineage.getDatasourceId());
                    dataSetNode.put("datasourceName", lineage.getDatasourceName());
                    dataSetNode.put("children", new ArrayList<>());
                    dataSetNodes.put(dataSetId, dataSetNode);
                    ((List<Map<String, Object>>) root.get("children")).add(dataSetNode);
                }

                String tableKey = lineage.getDatasourceId() + "_" + lineage.getTableName();
                Map<String, Object> tableNode = tableNodes.get(tableKey);
                if (tableNode == null && lineage.getTableName() != null) {
                    tableNode = new LinkedHashMap<>();
                    tableNode.put("id", "table_" + tableKey);
                    tableNode.put("name", lineage.getTableName());
                    tableNode.put("type", "table");
                    tableNode.put("databaseName", lineage.getDatabaseName());
                    tableNode.put("schemaName", lineage.getSchemaName());
                    tableNode.put("datasourceName", lineage.getDatasourceName());
                    tableNode.put("children", new ArrayList<>());
                    tableNodes.put(tableKey, tableNode);
                    ((List<Map<String, Object>>) dataSetNode.get("children")).add(tableNode);
                }

                if (tableNode != null && lineage.getColumnName() != null) {
                    Map<String, Object> columnNode = new LinkedHashMap<>();
                    columnNode.put("id", "column_" + tableKey + "_" + lineage.getColumnName());
                    columnNode.put("name", lineage.getColumnName());
                    columnNode.put("type", "column");
                    columnNode.put("lineageType", lineage.getLineageType());
                    columnNode.put("expression", lineage.getExpression());

                    boolean exists = false;
                    for (Map<String, Object> child : (List<Map<String, Object>>) tableNode.get("children")) {
                        if (lineage.getColumnName().equals(child.get("name"))) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        ((List<Map<String, Object>>) tableNode.get("children")).add(columnNode);
                    }
                }

                ((List<Map<String, Object>>) dataSetNode.get("children")).add(fieldNode);
            }

            result.put("success", true);
            result.put("reportId", reportId);
            result.put("tree", root);
            result.put("lineageCount", lineages.size());
            result.put("dataSetCount", dataSetNodes.size());
            result.put("tableCount", tableNodes.size());
        } catch (Exception e) {
            logger.error("获取血缘树失败: reportId={}", reportId, e);
            result.put("success", false);
            result.put("message", "获取血缘树失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getLineageTrace(Long reportId, String reportField) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            DataLineage lineage = lineageMapper.selectByReportField(reportId, reportField);
            if (lineage == null) {
                result.put("success", false);
                result.put("message", "未找到该字段的血缘关系");
                return result;
            }

            List<Map<String, Object>> trace = new ArrayList<>();

            Map<String, Object> reportNode = new LinkedHashMap<>();
            reportNode.put("level", 1);
            reportNode.put("type", "report");
            reportNode.put("name", lineage.getReportName());
            reportNode.put("field", lineage.getReportField());
            reportNode.put("title", lineage.getReportFieldTitle());
            trace.add(reportNode);

            Map<String, Object> dataSetNode = new LinkedHashMap<>();
            dataSetNode.put("level", 2);
            dataSetNode.put("type", "dataSet");
            dataSetNode.put("name", lineage.getDataSetName());
            dataSetNode.put("field", lineage.getDataSetField());
            dataSetNode.put("expression", lineage.getExpression());
            dataSetNode.put("lineageType", lineage.getLineageType());
            trace.add(dataSetNode);

            if (lineage.getTableName() != null || lineage.getColumnName() != null) {
                Map<String, Object> dbNode = new LinkedHashMap<>();
                dbNode.put("level", 3);
                dbNode.put("type", "database");
                dbNode.put("datasourceName", lineage.getDatasourceName());
                dbNode.put("databaseName", lineage.getDatabaseName());
                dbNode.put("tableName", lineage.getTableName());
                dbNode.put("columnName", lineage.getColumnName());
                dbNode.put("sqlText", lineage.getSqlText());
                trace.add(dbNode);
            }

            result.put("success", true);
            result.put("reportId", reportId);
            result.put("reportField", reportField);
            result.put("trace", trace);
            result.put("lineage", lineage);
        } catch (Exception e) {
            logger.error("获取血缘追溯失败: reportId={}, reportField={}", reportId, reportField, e);
            result.put("success", false);
            result.put("message", "获取血缘追溯失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLineageByReport(Long reportId) {
        return lineageMapper.deleteByReportId(reportId) >= 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLineageByDataSet(Long dataSetId) {
        return lineageMapper.deleteByDataSetId(dataSetId) >= 0;
    }

    @SuppressWarnings("unchecked")
    private int buildLineageForTemplate(ReportTemplate template) throws Exception {
        int count = 0;
        String dataSetBindJson = template.getDataSetBind();

        if (dataSetBindJson == null || dataSetBindJson.trim().isEmpty()) {
            logger.warn("报表无数据集绑定: reportId={}", template.getId());
            return 0;
        }

        List<Map<String, Object>> bindings;
        try {
            bindings = JSON.parseObject(dataSetBindJson,
                    new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            logger.error("解析数据集绑定失败: reportId={}", template.getId(), e);
            return 0;
        }

        Map<String, DataSet> dataSetCache = new HashMap<>();
        Map<Long, DataSourceConfig> dataSourceCache = new HashMap<>();

        for (Map<String, Object> binding : bindings) {
            Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
            String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "default";

            DataSet dataSet = dataSetCache.computeIfAbsent(dataSetId, id -> dataSetService.getById(id));
            if (dataSet == null) continue;

            DataSourceConfig dataSource = dataSourceCache.computeIfAbsent(
                    dataSet.getDsId(), id -> dataSourceConfigService.getById(id));

            SqlParseUtils.ParseResult parseResult = SqlParseUtils.parseSql(dataSet.getSqlText());

            List<Map<String, Object>> fields = (List<Map<String, Object>>) binding.get("fields");
            if (fields == null) fields = Collections.emptyList();

            for (Map<String, Object> field : fields) {
                String reportField = field.get("reportField") != null ? field.get("reportField").toString() : null;
                String reportFieldTitle = field.get("title") != null ? field.get("title").toString() : reportField;
                String dataSetField = field.get("dataSetField") != null ? field.get("dataSetField").toString() : reportField;
                String expression = field.get("expression") != null ? field.get("expression").toString() : dataSetField;

                if (reportField == null) continue;

                String lineageType = "DIRECT";
                if (SqlParseUtils.isAggregationExpression(expression)) {
                    lineageType = "AGGREGATION";
                } else if (!expression.equals(dataSetField)) {
                    lineageType = "EXPRESSION";
                }

                String matchingColumn = findMatchingColumn(dataSetField, parseResult);
                String mainTable = parseResult.getMainTable();

                DataLineage lineage = new DataLineage();
                lineage.setReportId(template.getId());
                lineage.setReportName(template.getTemplateName());
                lineage.setReportField(reportField);
                lineage.setReportFieldTitle(reportFieldTitle);
                lineage.setDataSetId(dataSet.getId());
                lineage.setDataSetName(dataSet.getSetName());
                lineage.setDataSetField(dataSetField);
                lineage.setBindName(bindName);
                lineage.setExpression(expression);
                lineage.setLineageType(lineageType);

                if (dataSource != null) {
                    lineage.setDatasourceId(dataSource.getId());
                    lineage.setDatasourceName(dataSource.getDsName());
                    lineage.setDatasourceType(dataSource.getDsType());
                    lineage.setDatabaseName(SqlParseUtils.extractDatabaseNameFromJdbcUrl(dataSource.getJdbcUrl()));
                    lineage.setSchemaName(dataSource.getSchemaName());
                }

                lineage.setTableName(mainTable);
                lineage.setColumnName(matchingColumn);
                lineage.setSourceTables(JSON.toJSONString(parseResult.getTables()));
                lineage.setSourceColumns(JSON.toJSONString(parseResult.getColumns()));
                lineage.setSqlText(dataSet.getSqlText());
                lineage.setStatus(1);
                lineage.setCreateTime(LocalDateTime.now());
                lineage.setUpdateTime(LocalDateTime.now());

                String hashSource = template.getId() + "_" + reportField + "_" + dataSetId + "_" + dataSetField;
                lineage.setLineageHash(SqlParseUtils.calculateHash(hashSource));

                if (lineageMapper.countByLineageHash(lineage.getLineageHash()) == 0) {
                    lineageMapper.insert(lineage);
                    count++;
                }
            }
        }

        return count;
    }

    private String findMatchingColumn(String dataSetField, SqlParseUtils.ParseResult parseResult) {
        if (dataSetField == null) return null;

        for (String col : parseResult.getSelectColumns()) {
            if (dataSetField.equalsIgnoreCase(col)) {
                return col;
            }
        }

        for (String col : parseResult.getColumns()) {
            if (dataSetField.equalsIgnoreCase(col)) {
                return col;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ReportTemplate> findReportsUsingDataSet(Long dataSetId) {
        List<ReportTemplate> result = new ArrayList<>();
        List<ReportTemplate> allTemplates = reportTemplateService.list(
                new LambdaQueryWrapper<ReportTemplate>().eq(ReportTemplate::getStatus, 1));

        for (ReportTemplate template : allTemplates) {
            String bindJson = template.getDataSetBind();
            if (bindJson != null && bindJson.contains("\"dataSetId\":" + dataSetId)) {
                try {
                    List<Map<String, Object>> bindings = JSON.parseObject(bindJson,
                            new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> binding : bindings) {
                        if (String.valueOf(dataSetId).equals(String.valueOf(binding.get("dataSetId")))) {
                            result.add(template);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return result;
    }
}
