package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportDataSnapshot;
import com.openreport.admin.entity.ReportSnapshotConfig;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.mapper.ReportDataSnapshotMapper;
import com.openreport.admin.mapper.ReportSnapshotConfigMapper;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportDataSnapshotService;
import com.openreport.admin.service.ReportSnapshotConfigService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.config.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportSnapshotConfigServiceImpl extends ServiceImpl<ReportSnapshotConfigMapper, ReportSnapshotConfig>
        implements ReportSnapshotConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ReportSnapshotConfigServiceImpl.class);

    @Autowired
    private ReportSnapshotConfigMapper configMapper;

    @Autowired
    private ReportDataSnapshotMapper snapshotMapper;

    @Autowired
    private ReportDataSnapshotService dataSnapshotService;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Override
    public List<ReportSnapshotConfig> listEnabledConfigs() {
        return configMapper.selectEnabledConfigs();
    }

    @Override
    public List<ReportSnapshotConfig> listByReportId(Long reportId) {
        return configMapper.selectByReportId(reportId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportSnapshotConfig createConfig(ReportSnapshotConfig config) {
        ReportTemplate template = reportTemplateService.getById(config.getReportId());
        if (template != null) {
            config.setReportName(template.getTemplateName());
        }
        config.setEnabled(config.getEnabled() != null ? config.getEnabled() : 1);
        config.setRetentionDays(config.getRetentionDays() != null ? config.getRetentionDays() : 30);
        config.setSnapshotType(config.getSnapshotType() != null ? config.getSnapshotType() : "FULL");
        config.setStorageType(config.getStorageType() != null ? config.getStorageType() : "MYSQL");
        config.setSnapshotCount(0);
        config.setMaxSnapshots(config.getMaxSnapshots() != null ? config.getMaxSnapshots() : 100);
        config.setStatus(1);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        Long userId = SecurityContext.getUserId();
        if (userId != null) {
            config.setCreateBy(userId);
            config.setUpdateBy(userId);
            config.setCreateByName(SecurityContext.getUsername());
        }
        save(config);
        logger.info("创建快照配置成功, configId: {}, reportId: {}", config.getId(), config.getReportId());
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportSnapshotConfig updateConfig(ReportSnapshotConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        Long userId = SecurityContext.getUserId();
        if (userId != null) {
            config.setUpdateBy(userId);
        }
        updateById(config);
        logger.info("更新快照配置成功, configId: {}", config.getId());
        return getById(config.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        ReportSnapshotConfig config = getById(id);
        if (config == null) {
            return false;
        }
        configMapper.deleteById(id);
        LambdaQueryWrapper<ReportDataSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportDataSnapshot::getConfigId, id);
        snapshotMapper.delete(wrapper);
        logger.info("删除快照配置成功, configId: {}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long id, Integer enabled) {
        ReportSnapshotConfig config = getById(id);
        if (config == null) {
            return false;
        }
        config.setEnabled(enabled);
        config.setUpdateTime(LocalDateTime.now());
        return updateById(config);
    }

    @Override
    public ReportSnapshotConfig getByReportId(Long reportId) {
        LambdaQueryWrapper<ReportSnapshotConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSnapshotConfig::getReportId, reportId)
                .eq(ReportSnapshotConfig::getDeleted, 0)
                .orderByDesc(ReportSnapshotConfig::getCreateTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSnapshot(Long configId, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        ReportSnapshotConfig config = getById(configId);
        if (config == null) {
            result.put("success", false);
            result.put("message", "快照配置不存在");
            return result;
        }

        Long reportId = config.getReportId();
        ReportTemplate template = reportTemplateService.getById(reportId);
        if (template == null) {
            result.put("success", false);
            result.put("message", "报表模板不存在");
            return result;
        }

        Map<String, Object> snapshotParams = params;
        if (snapshotParams == null || snapshotParams.isEmpty()) {
            if (config.getParamsJson() != null) {
                try {
                    snapshotParams = JSON.parseObject(config.getParamsJson(), Map.class);
                } catch (Exception e) {
                    snapshotParams = new HashMap<>();
                }
            } else {
                snapshotParams = new HashMap<>();
            }
        }

        long startTime = System.currentTimeMillis();
        ReportDataSnapshot snapshot = new ReportDataSnapshot();
        snapshot.setReportId(reportId);
        snapshot.setReportName(template.getTemplateName());
        snapshot.setConfigId(configId);
        snapshot.setSnapshotName(config.getReportName() + "_" +
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()));
        snapshot.setSnapshotType(config.getSnapshotType());
        snapshot.setStorageType(config.getStorageType());
        snapshot.setDataVersion(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        snapshot.setParamsJson(JSON.toJSONString(snapshotParams));
        snapshot.setStatus(0);
        snapshot.setCreateTime(LocalDateTime.now());

        Long userId = SecurityContext.getUserId();
        if (userId != null) {
            snapshot.setCreateBy(userId);
            snapshot.setCreateByName(SecurityContext.getUsername());
        }

        try {
            Map<String, Object> executeResult = executeReportForSnapshot(template, snapshotParams);
            long executeTime = System.currentTimeMillis() - startTime;

            String dataJson = JSON.toJSONString(executeResult);
            long dataSize = dataJson.getBytes(StandardCharsets.UTF_8).length;

            String dataHash = calculateHash(dataJson);

            long rowCount = 0;
            int tableCount = 0;
            if (executeResult.get("tables") != null) {
                List<Map<String, Object>> tables = (List<Map<String, Object>>) executeResult.get("tables");
                tableCount = tables.size();
                for (Map<String, Object> table : tables) {
                    if (table.get("total") != null) {
                        rowCount += Long.parseLong(table.get("total").toString());
                    } else if (table.get("rows") != null) {
                        List<?> rows = (List<?>) table.get("rows");
                        rowCount += rows.size();
                    }
                }
            }

            LocalDateTime expireTime = LocalDateTime.now().plusDays(
                    config.getRetentionDays() != null ? config.getRetentionDays() : 30);

            snapshot.setDataJson(dataJson);
            snapshot.setDataSize(dataSize);
            snapshot.setRowCount(rowCount);
            snapshot.setTableCount(tableCount);
            snapshot.setExecuteTime(executeTime);
            snapshot.setDataHash(dataHash);
            snapshot.setExpireTime(expireTime);
            snapshot.setStatus(1);
            snapshotMapper.insert(snapshot);

            config.setLastSnapshotId(snapshot.getId());
            config.setLastSnapshotTime(LocalDateTime.now());
            config.setSnapshotCount(config.getSnapshotCount() == null ? 1 : config.getSnapshotCount() + 1);
            config.setUpdateTime(LocalDateTime.now());
            updateById(config);

            enforceMaxSnapshots(config);

            result.put("success", true);
            result.put("snapshotId", snapshot.getId());
            result.put("snapshotName", snapshot.getSnapshotName());
            result.put("dataVersion", snapshot.getDataVersion());
            result.put("rowCount", rowCount);
            result.put("dataSize", dataSize);
            result.put("executeTime", executeTime);
            result.put("expireTime", expireTime);
            logger.info("创建快照成功, snapshotId: {}, reportId: {}, rowCount: {}", snapshot.getId(), reportId, rowCount);

        } catch (Exception e) {
            logger.error("创建快照失败, configId: {}", configId, e);
            snapshot.setStatus(-1);
            snapshot.setErrorMsg(e.getMessage());
            snapshotMapper.insert(snapshot);
            result.put("success", false);
            result.put("message", "创建快照失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> executeReportForSnapshot(ReportTemplate template, Map<String, Object> params) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("templateJson", template.getTemplateJson());

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
        result.put("title", template.getTemplateName());
        result.put("summary", template.getDescription());
        result.put("charts", new ArrayList<>());
        result.put("html", null);

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

    private void enforceMaxSnapshots(ReportSnapshotConfig config) {
        Integer maxSnapshots = config.getMaxSnapshots();
        if (maxSnapshots == null || maxSnapshots <= 0) {
            return;
        }
        List<ReportDataSnapshot> snapshots = snapshotMapper.selectByConfigId(config.getId());
        if (snapshots.size() > maxSnapshots) {
            List<ReportDataSnapshot> toDelete = snapshots.subList(maxSnapshots, snapshots.size());
            for (ReportDataSnapshot s : toDelete) {
                snapshotMapper.deleteById(s.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cleanupExpiredSnapshots() {
        try {
            int deletedCount = snapshotMapper.deleteExpiredSnapshots(LocalDateTime.now());
            logger.info("清理过期快照完成, 删除数量: {}", deletedCount);
            return true;
        } catch (Exception e) {
            logger.error("清理过期快照失败", e);
            return false;
        }
    }

    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
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
