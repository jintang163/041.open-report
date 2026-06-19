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

            List<Map<String, Object>> impactSummaries = new ArrayList<>();
            List<DataLineage> newLineages = lineageMapper.selectByReportId(reportId);
            Map<Long, Set<String>> dsTableMap = new LinkedHashMap<>();
            for (DataLineage lineage : newLineages) {
                if (lineage.getDatasourceId() != null && lineage.getTableName() != null) {
                    dsTableMap.computeIfAbsent(lineage.getDatasourceId(), k -> new LinkedHashSet<>())
                            .add(lineage.getTableName());
                }
            }
            for (Map.Entry<Long, Set<String>> entry : dsTableMap.entrySet()) {
                Long dsId = entry.getKey();
                for (String tableName : entry.getValue()) {
                    List<DataLineage> affected = lineageMapper.selectAffectedReports(dsId, tableName, null);
                    if (!affected.isEmpty()) {
                        Map<String, Object> summary = new LinkedHashMap<>();
                        summary.put("datasourceId", dsId);
                        summary.put("tableName", tableName);
                        summary.put("affectedReportCount", affected.size());
                        impactSummaries.add(summary);
                    }
                }
            }

            result.put("success", true);
            result.put("reportId", reportId);
            result.put("reportName", template.getTemplateName());
            result.put("lineageCount", lineageCount);
            result.put("impactSummaries", impactSummaries);
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

            List<Map<String, Object>> impactSummaries = new ArrayList<>();
            if (!templates.isEmpty() && dataSet.getDsId() != null) {
                DataSourceConfig dataSource = dataSourceConfigService.getById(dataSet.getDsId());
                if (dataSource != null) {
                    SqlParseUtils.ParseResult parseResult = SqlParseUtils.parseSql(dataSet.getSqlText());
                    for (String table : parseResult.getTables()) {
                        List<DataLineage> affected = lineageMapper.selectAffectedReports(
                                dataSource.getId(), table, null);
                        if (!affected.isEmpty()) {
                            Map<String, Object> summary = new LinkedHashMap<>();
                            summary.put("datasourceId", dataSource.getId());
                            summary.put("datasourceName", dataSource.getDsName());
                            summary.put("tableName", table);
                            summary.put("affectedReportCount", affected.size());
                            List<Map<String, Object>> reportSummaries = new ArrayList<>();
                            for (DataLineage al : affected) {
                                Map<String, Object> rs = new LinkedHashMap<>();
                                rs.put("reportId", al.getReportId());
                                rs.put("reportName", al.getReportName());
                                rs.put("reportField", al.getReportField());
                                reportSummaries.add(rs);
                            }
                            summary.put("affectedReports", reportSummaries);
                            impactSummaries.add(summary);
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("dataSetId", dataSetId);
            result.put("dataSetName", dataSet.getSetName());
            result.put("affectedReportCount", templates.size());
            result.put("lineageCount", totalLineage);
            result.put("impactSummaries", impactSummaries);
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

        String templateJson = template.getTemplateJson();
        String dataSetBindJson = template.getDataSetBind();

        Map<String, DataSet> dataSetCache = new HashMap<>();
        Map<Long, DataSourceConfig> dataSourceCache = new HashMap<>();
        Map<String, Object> templateData = null;

        if (templateJson != null && !templateJson.trim().isEmpty()) {
            try {
                templateData = JSON.parseObject(templateJson,
                        new com.alibaba.fastjson.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.warn("解析templateJson失败: reportId={}", template.getId(), e);
            }
        }

        List<Map<String, Object>> datasetsFromTemplate = Collections.emptyList();
        if (templateData != null && templateData.get("datasets") != null) {
            datasetsFromTemplate = (List<Map<String, Object>>) templateData.get("datasets");
        }

        Map<String, Map<String, Object>> datasetByNameOrId = new LinkedHashMap<>();
        for (Map<String, Object> ds : datasetsFromTemplate) {
            String dsId = ds.get("id") != null ? String.valueOf(ds.get("id")) : null;
            String dsName = ds.get("name") != null ? ds.get("name").toString() : null;
            if (dsId != null) datasetByNameOrId.put(dsId, ds);
            if (dsName != null) datasetByNameOrId.put(dsName, ds);
        }

        Map<String, String> bindNameToDataSetId = new LinkedHashMap<>();
        if (dataSetBindJson != null && !dataSetBindJson.trim().isEmpty()) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(dataSetBindJson,
                        new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    String bName = binding.get("bindName") != null ? binding.get("bindName").toString() : "default";
                    String dsId = binding.get("dataSetId") != null ? String.valueOf(binding.get("dataSetId")) : null;
                    if (dsId != null) {
                        bindNameToDataSetId.put(bName, dsId);
                    }
                }
            } catch (Exception e) {
                logger.warn("解析dataSetBind失败: reportId={}", template.getId(), e);
            }
        }

        Set<String> processedFields = new HashSet<>();

        if (templateData != null) {
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) templateData.get("sheets");
            if (sheets != null) {
                for (Map<String, Object> sheet : sheets) {
                    List<Map<String, Object>> cells = (List<Map<String, Object>>) sheet.get("cells");
                    if (cells == null) continue;

                    for (Map<String, Object> cell : cells) {
                        Map<String, Object> dataBinding = (Map<String, Object>) cell.get("dataBinding");
                        if (dataBinding == null) continue;

                        String type = dataBinding.get("type") != null ? dataBinding.get("type").toString() : "";
                        if (!"field".equals(type) && !"expression".equals(type)) continue;

                        String datasetBindName = dataBinding.get("dataset") != null ? dataBinding.get("dataset").toString() : null;
                        String field = dataBinding.get("field") != null ? dataBinding.get("field").toString() : null;
                        String expression = dataBinding.get("expression") != null ? dataBinding.get("expression").toString() : null;
                        String format = dataBinding.get("format") != null ? dataBinding.get("format").toString() : null;

                        if (field == null && expression == null) continue;

                        String cellRef = "R" + cell.get("row") + "C" + cell.get("col");
                        String reportField = field != null ? field : cellRef;
                        String dedupeKey = reportField + "_" + datasetBindName;
                        if (processedFields.contains(dedupeKey)) continue;
                        processedFields.add(dedupeKey);

                        DataSet dataSet = null;
                        String bindName = datasetBindName != null ? datasetBindName : "default";

                        if (datasetBindName != null && bindNameToDataSetId.containsKey(datasetBindName)) {
                            Long dsId = Long.valueOf(bindNameToDataSetId.get(datasetBindName));
                            dataSet = dataSetCache.computeIfAbsent(dsId, id -> dataSetService.getById(id));
                        } else if (datasetBindName != null && datasetByNameOrId.containsKey(datasetBindName)) {
                            Map<String, Object> dsInfo = datasetByNameOrId.get(datasetBindName);
                            if (dsInfo.get("id") != null) {
                                Long dsId = Long.valueOf(dsInfo.get("id").toString());
                                dataSet = dataSetCache.computeIfAbsent(dsId, id -> dataSetService.getById(id));
                            }
                        } else if (!bindNameToDataSetId.isEmpty()) {
                            Map.Entry<String, String> firstEntry = bindNameToDataSetId.entrySet().iterator().next();
                            Long dsId = Long.valueOf(firstEntry.getValue());
                            dataSet = dataSetCache.computeIfAbsent(dsId, id -> dataSetService.getById(id));
                            bindName = firstEntry.getKey();
                        }

                        if (dataSet == null) {
                            logger.debug("未找到数据集: reportId={}, dataset={}", template.getId(), datasetBindName);
                            continue;
                        }

                        DataSourceConfig dataSource = dataSourceCache.computeIfAbsent(
                                dataSet.getDsId(), id -> dataSourceConfigService.getById(id));

                        SqlParseUtils.ParseResult parseResult = SqlParseUtils.parseSql(dataSet.getSqlText());

                        String dataSetField = field != null ? field : expression;
                        String lineageType = "DIRECT";
                        if ("expression".equals(type) || (expression != null && !expression.equals(field))) {
                            if (SqlParseUtils.isAggregationExpression(expression)) {
                                lineageType = "AGGREGATION";
                            } else {
                                lineageType = "EXPRESSION";
                            }
                            dataSetField = expression;
                        }

                        String matchingColumn = findMatchingColumn(dataSetField, parseResult);
                        String mainTable = parseResult.getMainTable();

                        DataLineage lineage = new DataLineage();
                        lineage.setReportId(template.getId());
                        lineage.setReportName(template.getTemplateName());
                        lineage.setReportField(reportField);
                        lineage.setReportFieldTitle(reportField);
                        lineage.setDataSetId(dataSet.getId());
                        lineage.setDataSetName(dataSet.getSetName());
                        lineage.setDataSetField(dataSetField);
                        lineage.setBindName(bindName);
                        lineage.setExpression(expression != null ? expression : dataSetField);
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

                        String hashSource = template.getId() + "_" + reportField + "_" + dataSet.getId() + "_" + dataSetField;
                        lineage.setLineageHash(SqlParseUtils.calculateHash(hashSource));

                        if (lineageMapper.countByLineageHash(lineage.getLineageHash()) == 0) {
                            lineageMapper.insert(lineage);
                            count++;
                        }
                    }
                }
            }
        }

        if (count == 0 && templateData != null) {
            List<Map<String, Object>> charts = (List<Map<String, Object>>) templateData.get("charts");
            if (charts != null) {
                for (Map<String, Object> chart : charts) {
                    String dsRef = chart.get("datasetId") != null ? chart.get("datasetId").toString() : null;
                    if (dsRef == null) continue;

                    DataSet dataSet = dataSetCache.computeIfAbsent(Long.valueOf(dsRef), id -> dataSetService.getById(id));
                    if (dataSet == null) continue;

                    DataSourceConfig dataSource = dataSourceCache.computeIfAbsent(
                            dataSet.getDsId(), id -> dataSourceConfigService.getById(id));

                    SqlParseUtils.ParseResult parseResult = SqlParseUtils.parseSql(dataSet.getSqlText());

                    String chartTitle = chart.get("title") != null ? chart.get("title").toString() : "chart_" + chart.hashCode();
                    String chartType = chart.get("chartType") != null ? chart.get("chartType").toString() : "unknown";

                    for (String col : parseResult.getSelectColumns()) {
                        if (col.equals("*")) continue;
                        String dedupeKey = col + "_chart_" + dsRef;
                        if (processedFields.contains(dedupeKey)) continue;
                        processedFields.add(dedupeKey);

                        DataLineage lineage = new DataLineage();
                        lineage.setReportId(template.getId());
                        lineage.setReportName(template.getTemplateName());
                        lineage.setReportField(col);
                        lineage.setReportFieldTitle(col);
                        lineage.setDataSetId(dataSet.getId());
                        lineage.setDataSetName(dataSet.getSetName());
                        lineage.setDataSetField(col);
                        lineage.setBindName("chart_" + chartTitle);
                        lineage.setExpression(col);
                        lineage.setLineageType("DIRECT");

                        if (dataSource != null) {
                            lineage.setDatasourceId(dataSource.getId());
                            lineage.setDatasourceName(dataSource.getDsName());
                            lineage.setDatasourceType(dataSource.getDsType());
                            lineage.setDatabaseName(SqlParseUtils.extractDatabaseNameFromJdbcUrl(dataSource.getJdbcUrl()));
                            lineage.setSchemaName(dataSource.getSchemaName());
                        }

                        lineage.setTableName(parseResult.getMainTable());
                        lineage.setColumnName(findMatchingColumn(col, parseResult));
                        lineage.setSourceTables(JSON.toJSONString(parseResult.getTables()));
                        lineage.setSourceColumns(JSON.toJSONString(parseResult.getColumns()));
                        lineage.setSqlText(dataSet.getSqlText());
                        lineage.setStatus(1);
                        lineage.setCreateTime(LocalDateTime.now());
                        lineage.setUpdateTime(LocalDateTime.now());

                        String hashSource = template.getId() + "_" + col + "_" + dataSet.getId() + "_" + col;
                        lineage.setLineageHash(SqlParseUtils.calculateHash(hashSource));

                        if (lineageMapper.countByLineageHash(lineage.getLineageHash()) == 0) {
                            lineageMapper.insert(lineage);
                            count++;
                        }
                    }
                }
            }
        }

        if (count == 0) {
            count += buildLineageFromFieldConfig(template, dataSetCache, dataSourceCache, processedFields);
        }

        return count;
    }

    @SuppressWarnings("unchecked")
    private int buildLineageFromFieldConfig(ReportTemplate template,
                                             Map<String, DataSet> dataSetCache,
                                             Map<Long, DataSourceConfig> dataSourceCache,
                                             Set<String> processedFields) throws Exception {
        int count = 0;
        String dataSetBindJson = template.getDataSetBind();

        if (dataSetBindJson == null || dataSetBindJson.trim().isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> bindings;
        try {
            bindings = JSON.parseObject(dataSetBindJson,
                    new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return 0;
        }

        for (Map<String, Object> binding : bindings) {
            Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
            String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "default";

            DataSet dataSet = dataSetCache.computeIfAbsent(dataSetId, id -> dataSetService.getById(id));
            if (dataSet == null) continue;

            DataSourceConfig dataSource = dataSourceCache.computeIfAbsent(
                    dataSet.getDsId(), id -> dataSourceConfigService.getById(id));

            SqlParseUtils.ParseResult parseResult = SqlParseUtils.parseSql(dataSet.getSqlText());

            List<Map<String, Object>> fieldConfigList = Collections.emptyList();
            if (dataSet.getFieldConfig() != null && !dataSet.getFieldConfig().trim().isEmpty()) {
                try {
                    fieldConfigList = JSON.parseObject(dataSet.getFieldConfig(),
                            new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    logger.warn("解析fieldConfig失败: dataSetId={}", dataSetId, e);
                }
            }

            if (fieldConfigList.isEmpty() && !parseResult.getSelectColumns().isEmpty()) {
                for (String col : parseResult.getSelectColumns()) {
                    if (col.equals("*")) continue;
                    String dedupeKey = col + "_" + bindName;
                    if (processedFields.contains(dedupeKey)) continue;
                    processedFields.add(dedupeKey);

                    count += insertLineageRecord(template, dataSet, dataSource,
                            parseResult, col, col, col, "DIRECT", bindName);
                }
            } else {
                for (Map<String, Object> fieldConf : fieldConfigList) {
                    String fieldName = fieldConf.get("name") != null ? fieldConf.get("name").toString() : null;
                    String fieldLabel = fieldConf.get("label") != null ? fieldConf.get("label").toString() : fieldName;
                    if (fieldName == null) continue;

                    String dedupeKey = fieldName + "_" + bindName;
                    if (processedFields.contains(dedupeKey)) continue;
                    processedFields.add(dedupeKey);

                    count += insertLineageRecord(template, dataSet, dataSource,
                            parseResult, fieldName, fieldLabel, fieldName, "DIRECT", bindName);
                }
            }
        }

        return count;
    }

    private int insertLineageRecord(ReportTemplate template, DataSet dataSet,
                                     DataSourceConfig dataSource,
                                     SqlParseUtils.ParseResult parseResult,
                                     String reportField, String reportFieldTitle,
                                     String dataSetField, String lineageType,
                                     String bindName) {
        String matchingColumn = findMatchingColumn(dataSetField, parseResult);

        DataLineage lineage = new DataLineage();
        lineage.setReportId(template.getId());
        lineage.setReportName(template.getTemplateName());
        lineage.setReportField(reportField);
        lineage.setReportFieldTitle(reportFieldTitle);
        lineage.setDataSetId(dataSet.getId());
        lineage.setDataSetName(dataSet.getSetName());
        lineage.setDataSetField(dataSetField);
        lineage.setBindName(bindName);
        lineage.setExpression(dataSetField);
        lineage.setLineageType(lineageType);

        if (dataSource != null) {
            lineage.setDatasourceId(dataSource.getId());
            lineage.setDatasourceName(dataSource.getDsName());
            lineage.setDatasourceType(dataSource.getDsType());
            lineage.setDatabaseName(SqlParseUtils.extractDatabaseNameFromJdbcUrl(dataSource.getJdbcUrl()));
            lineage.setSchemaName(dataSource.getSchemaName());
        }

        lineage.setTableName(parseResult.getMainTable());
        lineage.setColumnName(matchingColumn);
        lineage.setSourceTables(JSON.toJSONString(parseResult.getTables()));
        lineage.setSourceColumns(JSON.toJSONString(parseResult.getColumns()));
        lineage.setSqlText(dataSet.getSqlText());
        lineage.setStatus(1);
        lineage.setCreateTime(LocalDateTime.now());
        lineage.setUpdateTime(LocalDateTime.now());

        String hashSource = template.getId() + "_" + reportField + "_" + dataSet.getId() + "_" + dataSetField;
        lineage.setLineageHash(SqlParseUtils.calculateHash(hashSource));

        if (lineageMapper.countByLineageHash(lineage.getLineageHash()) == 0) {
            lineageMapper.insert(lineage);
            return 1;
        }
        return 0;
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
