package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportDataSnapshot;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.mapper.ReportDataSnapshotMapper;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportDataSnapshotService;
import com.openreport.admin.service.ReportTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportDataSnapshotServiceImpl extends ServiceImpl<ReportDataSnapshotMapper, ReportDataSnapshot>
        implements ReportDataSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(ReportDataSnapshotServiceImpl.class);

    @Autowired
    private ReportDataSnapshotMapper snapshotMapper;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Override
    public List<ReportDataSnapshot> listByReportId(Long reportId, Integer limit) {
        return snapshotMapper.selectByReportId(reportId, limit != null ? limit : 50);
    }

    @Override
    public List<ReportDataSnapshot> listByConfigId(Long configId) {
        return snapshotMapper.selectByConfigId(configId);
    }

    @Override
    public ReportDataSnapshot getLatestByReportId(Long reportId) {
        return snapshotMapper.selectLatestByReportId(reportId);
    }

    @Override
    public List<ReportDataSnapshot> listExpiredSnapshots() {
        return snapshotMapper.selectExpiredSnapshots(LocalDateTime.now());
    }

    @Override
    public int deleteExpiredSnapshots() {
        return snapshotMapper.deleteExpiredSnapshots(LocalDateTime.now());
    }

    @Override
    public List<ReportDataSnapshot> listByReportIdAndTimeRange(Long reportId, LocalDateTime startTime, LocalDateTime endTime) {
        return snapshotMapper.selectByReportIdAndTimeRange(reportId, startTime, endTime);
    }

    @Override
    public Map<String, Object> loadSnapshotData(Long snapshotId) {
        ReportDataSnapshot snapshot = getById(snapshotId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (snapshot == null) {
            result.put("success", false);
            result.put("message", "快照不存在");
            return result;
        }
        if (snapshot.getStatus() != null && snapshot.getStatus() == -1) {
            result.put("success", false);
            result.put("message", "快照生成失败: " + snapshot.getErrorMsg());
            return result;
        }
        if (snapshot.getExpireTime() != null && snapshot.getExpireTime().isBefore(LocalDateTime.now())) {
            result.put("success", false);
            result.put("message", "快照已过期");
            return result;
        }

        try {
            Map<String, Object> data = JSON.parseObject(snapshot.getDataJson(), Map.class);
            result.put("success", true);
            result.put("snapshotId", snapshot.getId());
            result.put("snapshotName", snapshot.getSnapshotName());
            result.put("dataVersion", snapshot.getDataVersion());
            result.put("snapshotTime", snapshot.getCreateTime());
            result.put("expireTime", snapshot.getExpireTime());
            result.put("isSnapshot", true);
            result.putAll(data);
            logger.info("加载快照数据成功, snapshotId: {}", snapshotId);
        } catch (Exception e) {
            logger.error("解析快照数据失败, snapshotId: {}", snapshotId, e);
            result.put("success", false);
            result.put("message", "解析快照数据失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> compareSnapshots(Long baseSnapshotId, Long targetSnapshotId) {
        Map<String, Object> result = new LinkedHashMap<>();

        ReportDataSnapshot baseSnapshot = getById(baseSnapshotId);
        ReportDataSnapshot targetSnapshot = getById(targetSnapshotId);

        if (baseSnapshot == null) {
            result.put("success", false);
            result.put("message", "基准快照不存在");
            return result;
        }
        if (targetSnapshot == null) {
            result.put("success", false);
            result.put("message", "对比快照不存在");
            return result;
        }

        try {
            Map<String, Object> baseData = JSON.parseObject(baseSnapshot.getDataJson(), Map.class);
            Map<String, Object> targetData = JSON.parseObject(targetSnapshot.getDataJson(), Map.class);

            result.put("success", true);
            result.put("baseSnapshot", buildSnapshotInfo(baseSnapshot));
            result.put("targetSnapshot", buildSnapshotInfo(targetSnapshot));

            List<Map<String, Object>> tablesComparison = compareTables(baseData, targetData);
            result.put("tablesComparison", tablesComparison);

            Map<String, Object> summary = buildComparisonSummary(baseSnapshot, targetSnapshot, tablesComparison);
            result.put("summary", summary);

            logger.info("快照对比完成, baseId: {}, targetId: {}", baseSnapshotId, targetSnapshotId);
        } catch (Exception e) {
            logger.error("快照对比失败, baseId: {}, targetId: {}", baseSnapshotId, targetSnapshotId, e);
            result.put("success", false);
            result.put("message", "对比失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> compareSnapshotWithRealtime(Long snapshotId, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();

        ReportDataSnapshot snapshot = getById(snapshotId);
        if (snapshot == null) {
            result.put("success", false);
            result.put("message", "快照不存在");
            return result;
        }

        ReportTemplate template = reportTemplateService.getById(snapshot.getReportId());
        if (template == null) {
            result.put("success", false);
            result.put("message", "报表模板不存在");
            return result;
        }

        try {
            Map<String, Object> snapshotData = JSON.parseObject(snapshot.getDataJson(), Map.class);

            Map<String, Object> realtimeData = executeRealtimeReport(template, params != null ? params : new HashMap<>());

            result.put("success", true);
            result.put("snapshotInfo", buildSnapshotInfo(snapshot));
            result.put("realtimeInfo", Map.of(
                    "name", "实时数据",
                    "time", LocalDateTime.now().toString()
            ));

            List<Map<String, Object>> tablesComparison = compareTables(snapshotData, realtimeData);
            result.put("tablesComparison", tablesComparison);

            long realtimeRowCount = 0;
            if (realtimeData.get("tables") != null) {
                List<Map<String, Object>> tables = (List<Map<String, Object>>) realtimeData.get("tables");
                for (Map<String, Object> table : tables) {
                    if (table.get("total") != null) {
                        realtimeRowCount += Long.parseLong(table.get("total").toString());
                    } else if (table.get("rows") != null) {
                        List<?> rows = (List<?>) table.get("rows");
                        realtimeRowCount += rows.size();
                    }
                }
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("snapshotRowCount", snapshot.getRowCount());
            summary.put("realtimeRowCount", realtimeRowCount);
            summary.put("rowDiff", realtimeRowCount - (snapshot.getRowCount() != null ? snapshot.getRowCount() : 0));
            summary.put("dataHashChanged", !Objects.equals(snapshot.getDataHash(), calculateHash(JSON.toJSONString(realtimeData))));
            result.put("summary", summary);
            result.put("realtimeData", realtimeData);

            logger.info("快照与实时数据对比完成, snapshotId: {}", snapshotId);
        } catch (Exception e) {
            logger.error("快照与实时数据对比失败, snapshotId: {}", snapshotId, e);
            result.put("success", false);
            result.put("message", "对比失败: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> buildSnapshotInfo(ReportDataSnapshot snapshot) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", snapshot.getId());
        info.put("name", snapshot.getSnapshotName());
        info.put("dataVersion", snapshot.getDataVersion());
        info.put("createTime", snapshot.getCreateTime());
        info.put("rowCount", snapshot.getRowCount());
        info.put("dataSize", snapshot.getDataSize());
        info.put("tableCount", snapshot.getTableCount());
        info.put("dataHash", snapshot.getDataHash());
        return info;
    }

    private List<Map<String, Object>> compareTables(Map<String, Object> baseData, Map<String, Object> targetData) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<Map<String, Object>> baseTables = (List<Map<String, Object>>) baseData.get("tables");
        List<Map<String, Object>> targetTables = (List<Map<String, Object>>) targetData.get("tables");

        if (baseTables == null) baseTables = new ArrayList<>();
        if (targetTables == null) targetTables = new ArrayList<>();

        int maxSize = Math.max(baseTables.size(), targetTables.size());
        for (int i = 0; i < maxSize; i++) {
            Map<String, Object> baseTable = i < baseTables.size() ? baseTables.get(i) : null;
            Map<String, Object> targetTable = i < targetTables.size() ? targetTables.get(i) : null;

            Map<String, Object> comparison = new LinkedHashMap<>();
            String bindName = baseTable != null && baseTable.get("bindName") != null
                    ? baseTable.get("bindName").toString()
                    : (targetTable != null && targetTable.get("bindName") != null
                    ? targetTable.get("bindName").toString()
                    : "table_" + i);
            comparison.put("bindName", bindName);

            long baseRows = 0, targetRows = 0;
            int baseCols = 0, targetCols = 0;
            List<Map<String, Object>> baseRowData = new ArrayList<>();
            List<Map<String, Object>> targetRowData = new ArrayList<>();
            List<Map<String, Object>> baseColDefs = new ArrayList<>();
            List<Map<String, Object>> targetColDefs = new ArrayList<>();

            if (baseTable != null) {
                if (baseTable.get("total") != null) {
                    baseRows = Long.parseLong(baseTable.get("total").toString());
                } else if (baseTable.get("rows") != null) {
                    baseRowData = (List<Map<String, Object>>) baseTable.get("rows");
                    baseRows = baseRowData.size();
                }
                if (baseTable.get("columns") != null) {
                    baseColDefs = (List<Map<String, Object>>) baseTable.get("columns");
                    baseCols = baseColDefs.size();
                }
            }
            if (targetTable != null) {
                if (targetTable.get("total") != null) {
                    targetRows = Long.parseLong(targetTable.get("total").toString());
                } else if (targetTable.get("rows") != null) {
                    targetRowData = (List<Map<String, Object>>) targetTable.get("rows");
                    targetRows = targetRowData.size();
                }
                if (targetTable.get("columns") != null) {
                    targetColDefs = (List<Map<String, Object>>) targetTable.get("columns");
                    targetCols = targetColDefs.size();
                }
            }

            comparison.put("baseRows", baseRows);
            comparison.put("targetRows", targetRows);
            comparison.put("rowDiff", targetRows - baseRows);
            comparison.put("rowDiffPercent", baseRows > 0 ? String.format("%.2f%%", (targetRows - baseRows) * 100.0 / baseRows) : "N/A");

            comparison.put("baseCols", baseCols);
            comparison.put("targetCols", targetCols);
            comparison.put("colDiff", targetCols - baseCols);

            List<Map<String, Object>> chartData = new ArrayList<>();
            int sampleSize = Math.min(20, (int) Math.max(baseRows, targetRows));

            Set<String> allKeys = new LinkedHashSet<>();
            for (int j = 0; j < Math.min(sampleSize, baseRowData.size()); j++) {
                allKeys.addAll(baseRowData.get(j).keySet());
            }
            for (int j = 0; j < Math.min(sampleSize, targetRowData.size()); j++) {
                allKeys.addAll(targetRowData.get(j).keySet());
            }

            String xField = null;
            String yField = null;
            for (String key : allKeys) {
                boolean isNumericBase = baseRowData.size() > 0 && isNumeric(baseRowData.get(0).get(key));
                boolean isNumericTarget = targetRowData.size() > 0 && isNumeric(targetRowData.get(0).get(key));
                if (xField == null && (!isNumericBase || !isNumericTarget)) {
                    xField = key;
                } else if (yField == null && (isNumericBase || isNumericTarget)) {
                    yField = key;
                }
            }
            if (xField == null && !allKeys.isEmpty()) {
                xField = allKeys.iterator().next();
            }
            if (yField == null && allKeys.size() > 1) {
                Iterator<String> it = allKeys.iterator();
                it.next();
                yField = it.next();
            }

            Map<String, Double> baseAgg = aggregateRows(baseRowData, xField, yField);
            Map<String, Double> targetAgg = aggregateRows(targetRowData, xField, yField);

            Set<String> xValues = new LinkedHashSet<>();
            xValues.addAll(baseAgg.keySet());
            xValues.addAll(targetAgg.keySet());
            List<String> xValueList = new ArrayList<>(xValues);
            if (xValueList.size() > 30) xValueList = xValueList.subList(0, 30);

            for (String xVal : xValueList) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("x", xVal);
                point.put("category", "base");
                point.put("value", baseAgg.getOrDefault(xVal, 0.0));
                chartData.add(point);

                Map<String, Object> point2 = new LinkedHashMap<>();
                point2.put("x", xVal);
                point2.put("category", "target");
                point2.put("value", targetAgg.getOrDefault(xVal, 0.0));
                chartData.add(point2);
            }

            comparison.put("xField", xField);
            comparison.put("yField", yField);
            comparison.put("chartData", chartData);

            result.add(comparison);
        }
        return result;
    }

    private Map<String, Double> aggregateRows(List<Map<String, Object>> rows, String xField, String yField) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (rows == null || xField == null || yField == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.getOrDefault(xField, ""));
            Object val = row.get(yField);
            double numVal = 0;
            if (val != null && isNumeric(val)) {
                numVal = Double.parseDouble(val.toString());
            }
            result.merge(key, numVal, Double::sum);
        }
        return result;
    }

    private boolean isNumeric(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Number) return true;
        try {
            Double.parseDouble(obj.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> buildComparisonSummary(ReportDataSnapshot base, ReportDataSnapshot target,
                                                        List<Map<String, Object>> tablesComparison) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("baseRowCount", base.getRowCount());
        summary.put("targetRowCount", target.getRowCount());
        long baseRows = base.getRowCount() != null ? base.getRowCount() : 0;
        long targetRows = target.getRowCount() != null ? target.getRowCount() : 0;
        summary.put("totalRowDiff", targetRows - baseRows);
        summary.put("rowDiffPercent", baseRows > 0 ? String.format("%.2f%%", (targetRows - baseRows) * 100.0 / baseRows) : "N/A");
        summary.put("dataHashChanged", !Objects.equals(base.getDataHash(), target.getDataHash()));
        summary.put("baseDataSize", base.getDataSize());
        summary.put("targetDataSize", target.getDataSize());
        summary.put("sizeDiff", (target.getDataSize() != null ? target.getDataSize() : 0) -
                (base.getDataSize() != null ? base.getDataSize() : 0));
        summary.put("hoursDiff", java.time.Duration.between(base.getCreateTime(), target.getCreateTime()).toHours());
        return summary;
    }

    private Map<String, Object> executeRealtimeReport(ReportTemplate template, Map<String, Object> params) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());

        List<Map<String, Object>> tables = new ArrayList<>();
        Map<String, Object> dataSetData = new LinkedHashMap<>();

        if (template.getDataSetBind() != null) {
            List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                    new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> binding : bindings) {
                Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                String bindName = binding.get("bindName") != null
                        ? binding.get("bindName").toString()
                        : "dataSet" + dataSetId;

                Map<String, Object> tableItem = new LinkedHashMap<>();
                List<Map<String, Object>> tableColumns = new ArrayList<>();
                List<Map<String, Object>> tableRows = new ArrayList<>();

                Map<String, Object> previewResult = dataSetService.previewDataWithCount(dataSetId, params, null);
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
                    tableItem.put("total", previewResult.get("total") != null ? previewResult.get("total") : (rows != null ? rows.size() : 0));
                }

                tableItem.put("bindName", bindName);
                tableItem.put("dataSetId", dataSetId);
                tableItem.put("columns", tableColumns);
                tableItem.put("rows", tableRows);
                tables.add(tableItem);
            }
        }

        result.put("tables", tables);
        result.put("dataSets", dataSetData);

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
        return result;
    }

    private String calculateHash(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32);
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
