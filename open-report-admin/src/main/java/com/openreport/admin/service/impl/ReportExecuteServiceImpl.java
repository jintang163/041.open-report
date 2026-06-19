package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportDataSnapshotService;
import com.openreport.admin.service.ReportExecuteService;
import com.openreport.admin.service.ReportTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ReportExecuteServiceImpl implements ReportExecuteService {

    private static final long BIG_DATA_THRESHOLD = 100_000L;
    private static final int BIG_DATA_FIRST_PAGE_SIZE = 200;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ReportDataSnapshotService reportDataSnapshotService;

    @Override
    public Map<String, Object> executeReport(Long templateId, Map<String, Object> params,
                                             String snapshotMode, Long snapshotId) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return buildErrorResult(templateId, "报表模板不存在");
        }

        if ("snapshot".equalsIgnoreCase(snapshotMode) && snapshotId != null) {
            Map<String, Object> snapshotData = reportDataSnapshotService.loadSnapshotData(snapshotId);
            if (Boolean.TRUE.equals(snapshotData.get("success"))) {
                return snapshotData;
            }
        }

        if ("latest".equalsIgnoreCase(snapshotMode)) {
            var latestSnapshot = reportDataSnapshotService.getLatestByReportId(templateId);
            if (latestSnapshot != null) {
                Map<String, Object> snapshotData = reportDataSnapshotService.loadSnapshotData(latestSnapshot.getId());
                if (Boolean.TRUE.equals(snapshotData.get("success"))) {
                    return snapshotData;
                }
            }
        }

        return buildReportResult(template, params);
    }

    @Override
    public Map<String, Object> executeReportInternal(Long templateId, Map<String, Object> params) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            return buildErrorResult(templateId, "报表模板不存在");
        }
        return buildReportResult(template, params);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildReportResult(ReportTemplate template, Map<String, Object> params) {
        Long templateId = template.getId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("templateJson", template.getTemplateJson());

        List<Map<String, Object>> tables = new ArrayList<>();
        Map<String, Object> dataSetData = new LinkedHashMap<>();
        boolean pageMode = false;
        long maxTotal = 0;
        Map<String, Object> pageInfo = new LinkedHashMap<>();

        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});

                int bindIdx = 0;
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null
                            ? binding.get("bindName").toString()
                            : "dataSet" + dataSetId;

                    long total = dataSetService.countData(dataSetId, params);
                    if (total > maxTotal) maxTotal = total;

                    Map<String, Object> tableItem = new LinkedHashMap<>();
                    List<Map<String, Object>> tableColumns = new ArrayList<>();
                    List<Map<String, Object>> tableRows = new ArrayList<>();

                    if (total >= 0 && total > BIG_DATA_THRESHOLD) {
                        pageMode = true;
                        Map<String, Object> pageResult = dataSetService.pagePreviewData(
                                dataSetId, params, 1, BIG_DATA_FIRST_PAGE_SIZE);
                        if (pageResult != null && Boolean.TRUE.equals(pageResult.get("success"))) {
                            List<Map<String, Object>> cols = (List<Map<String, Object>>) pageResult.get("columns");
                            List<Map<String, Object>> rows = (List<Map<String, Object>>) pageResult.get("rows");
                            if (cols != null) {
                                for (Map<String, Object> c : cols) {
                                    Map<String, Object> tc = new LinkedHashMap<>();
                                    tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                                    tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                                    tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                                    if (c.get("width") != null) tc.put("width", c.get("width"));
                                    if (c.get("align") != null) tc.put("align", c.get("align"));
                                    tableColumns.add(tc);
                                }
                            }
                            if (rows != null) tableRows.addAll(rows);

                            Boolean hasMore = (Boolean) pageResult.get("hasMore");
                            if (bindIdx == 0) {
                                pageInfo.put("columns", cols);
                                pageInfo.put("rows", rows);
                                pageInfo.put("total", pageResult.get("total") != null ? pageResult.get("total") : total);
                                pageInfo.put("pageNum", 1);
                                pageInfo.put("pageSize", BIG_DATA_FIRST_PAGE_SIZE);
                                pageInfo.put("hasMore", hasMore != null ? hasMore : (BIG_DATA_FIRST_PAGE_SIZE < total));
                                pageInfo.put("dataSetId", dataSetId);
                                pageInfo.put("bindName", bindName);
                            }
                        }
                    } else {
                        Map<String, Object> previewResult = dataSetService.previewDataWithCount(
                                dataSetId, params, null);
                        dataSetData.put(bindName, previewResult);
                        if (previewResult != null) {
                            List<Map<String, Object>> cols = (List<Map<String, Object>>) previewResult.get("columns");
                            List<Map<String, Object>> rows = (List<Map<String, Object>>) previewResult.get("rows");
                            if (cols != null) {
                                for (Map<String, Object> c : cols) {
                                    Map<String, Object> tc = new LinkedHashMap<>();
                                    tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                                    tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                                    tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                                    if (c.get("width") != null) tc.put("width", c.get("width"));
                                    if (c.get("align") != null) tc.put("align", c.get("align"));
                                    tableColumns.add(tc);
                                }
                            }
                            if (rows != null) tableRows.addAll(rows);
                            if (bindIdx == 0 && !pageMode) {
                                pageInfo.put("total", previewResult.get("total") != null ? previewResult.get("total") : (rows != null ? rows.size() : 0));
                            }
                        }
                    }

                    tableItem.put("bindName", bindName);
                    tableItem.put("dataSetId", dataSetId);
                    tableItem.put("columns", tableColumns);
                    tableItem.put("rows", tableRows);
                    tableItem.put("total", total);
                    tables.add(tableItem);

                    bindIdx++;
                }
            } catch (Exception e) {
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
                log.error("执行报表 {} 数据集绑定解析失败", templateId, e);
            }
        }

        result.put("tables", tables);
        result.put("dataSets", dataSetData);
        result.put("title", template.getTemplateName());
        result.put("summary", template.getDescription());
        result.put("charts", new ArrayList<>());
        result.put("html", null);

        if (pageMode) {
            result.put("pageMode", true);
            result.put("bigDataThreshold", BIG_DATA_THRESHOLD);
            result.put("page", pageInfo);
            Map<String, Object> tableData = new LinkedHashMap<>();
            List<Map<String, Object>> pCols = (List<Map<String, Object>>) pageInfo.get("columns");
            List<Map<String, Object>> tableDataColumns = new ArrayList<>();
            if (pCols != null) {
                for (Map<String, Object> c : pCols) {
                    Map<String, Object> tc = new LinkedHashMap<>();
                    tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                    tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                    tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                    if (c.get("width") != null) tc.put("width", c.get("width"));
                    if (c.get("align") != null) tc.put("align", c.get("align"));
                    tableDataColumns.add(tc);
                }
            }
            tableData.put("columns", tableDataColumns);
            tableData.put("dataSource", pageInfo.get("rows") != null ? pageInfo.get("rows") : new ArrayList<>());
            tableData.put("total", pageInfo.get("total"));
            tableData.put("pageNum", pageInfo.get("pageNum"));
            tableData.put("pageSize", pageInfo.get("pageSize"));
            result.put("table", tableData);
        } else {
            result.put("pageMode", false);
            if (!tables.isEmpty()) {
                Map<String, Object> first = tables.get(0);
                List<Map<String, Object>> tCols = (List<Map<String, Object>>) first.get("columns");
                List<Map<String, Object>> tRows = (List<Map<String, Object>>) first.get("rows");
                Map<String, Object> tableData = new LinkedHashMap<>();
                tableData.put("columns", tCols);
                tableData.put("dataSource", tRows);
                tableData.put("total", first.get("total"));
                result.put("table", tableData);
            }
        }

        result.put("success", true);
        return result;
    }

    private Map<String, Object> buildErrorResult(Long templateId, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
