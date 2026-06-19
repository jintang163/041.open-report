package com.openreport.admin.service.snapshot;

import com.alibaba.fastjson.JSON;
import com.openreport.admin.entity.ReportDataSnapshot;
import com.openreport.admin.mapper.ReportDataSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("mysqlDirectStorage")
public class MysqlDirectStorageStrategy implements SnapshotStorageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MysqlDirectStorageStrategy.class);

    @Autowired
    private ReportDataSnapshotMapper snapshotMapper;

    @Override
    public String getStorageType() {
        return "MYSQL";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void storeShard(Long snapshotId, Long reportId, Long configId,
                           String bindName, Long datasetId,
                           int shardIndex, int pageNum, int pageSize,
                           long startIndex, long endIndex,
                           List<Map<String, Object>> rows,
                           List<Map<String, Object>> columns) {
        logger.warn("MYSQL直接存储不支持分片存储，请使用 MYSQL_SHARD 存储类型");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadShard(Long snapshotId, String bindName, int shardIndex) {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadPage(Long snapshotId, String bindName, int pageNum, int pageSize) {
        ReportDataSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null || snapshot.getDataJson() == null) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> data = JSON.parseObject(snapshot.getDataJson(), Map.class);
            List<Map<String, Object>> tables = (List<Map<String, Object>>) data.get("tables");
            if (tables == null || tables.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, Object> targetTable = null;
            for (Map<String, Object> table : tables) {
                String name = table.get("bindName") != null ? table.get("bindName").toString() : "default";
                if (name.equals(bindName) || (bindName == null || bindName.isEmpty()) && targetTable == null) {
                    targetTable = table;
                    if (name.equals(bindName)) break;
                }
            }

            if (targetTable == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) targetTable.get("rows");
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }

            int startIdx = (pageNum - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, rows.size());
            if (startIdx >= rows.size()) {
                return Collections.emptyList();
            }
            return rows.subList(startIdx, endIdx);
        } catch (Exception e) {
            logger.error("分页加载快照数据失败: snapshotId={}", snapshotId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public long getTotalRows(Long snapshotId, String bindName) {
        ReportDataSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null || snapshot.getDataJson() == null) {
            return 0;
        }
        try {
            Map<String, Object> data = JSON.parseObject(snapshot.getDataJson(), Map.class);
            List<Map<String, Object>> tables = (List<Map<String, Object>>) data.get("tables");
            if (tables == null || tables.isEmpty()) {
                return 0;
            }

            for (Map<String, Object> table : tables) {
                String name = table.get("bindName") != null ? table.get("bindName").toString() : "default";
                if (name.equals(bindName) || (bindName == null || bindName.isEmpty())) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");
                    return rows != null ? rows.size() : 0;
                }
            }
            return 0;
        } catch (Exception e) {
            logger.error("获取总行数失败: snapshotId={}", snapshotId, e);
            return 0;
        }
    }

    @Override
    public int getTotalShards(Long snapshotId, String bindName) {
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listBindNames(Long snapshotId) {
        ReportDataSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null || snapshot.getDataJson() == null) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> data = JSON.parseObject(snapshot.getDataJson(), Map.class);
            List<Map<String, Object>> tables = (List<Map<String, Object>>) data.get("tables");
            if (tables == null || tables.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>();
            for (Map<String, Object> table : tables) {
                String name = table.get("bindName") != null ? table.get("bindName").toString() : "default";
                names.add(name);
            }
            return names;
        } catch (Exception e) {
            logger.error("获取数据集列表失败: snapshotId={}", snapshotId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getColumns(Long snapshotId, String bindName) {
        ReportDataSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null || snapshot.getDataJson() == null) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> data = JSON.parseObject(snapshot.getDataJson(), Map.class);
            List<Map<String, Object>> tables = (List<Map<String, Object>>) data.get("tables");
            if (tables == null || tables.isEmpty()) {
                return Collections.emptyList();
            }

            for (Map<String, Object> table : tables) {
                String name = table.get("bindName") != null ? table.get("bindName").toString() : "default";
                if (name.equals(bindName) || (bindName == null || bindName.isEmpty())) {
                    return (List<Map<String, Object>>) table.get("columns");
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("获取列信息失败: snapshotId={}", snapshotId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteSnapshotData(Long snapshotId) {
        logger.info("删除快照数据（直接存储）: snapshotId={}", snapshotId);
    }

    @Override
    public Map<String, Object> getStorageInfo(Long snapshotId) {
        Map<String, Object> info = new LinkedHashMap<>();
        ReportDataSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        info.put("storageType", getStorageType());
        info.put("totalShards", 1);
        info.put("totalRows", snapshot != null && snapshot.getRowCount() != null ? snapshot.getRowCount() : 0);
        info.put("totalSize", snapshot != null && snapshot.getDataSize() != null ? snapshot.getDataSize() : 0);
        info.put("bindNames", listBindNames(snapshotId));
        return info;
    }
}
