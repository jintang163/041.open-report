package com.openreport.admin.service.snapshot;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openreport.admin.entity.ReportSnapshotShard;
import com.openreport.admin.mapper.ReportSnapshotShardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service("mysqlShardStorage")
public class MysqlShardStorageStrategy implements SnapshotStorageStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MysqlShardStorageStrategy.class);

    @Autowired
    private ReportSnapshotShardMapper shardMapper;

    @Override
    public String getStorageType() {
        return "MYSQL_SHARD";
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
        try {
            ReportSnapshotShard shard = new ReportSnapshotShard();
            shard.setSnapshotId(snapshotId);
            shard.setReportId(reportId);
            shard.setConfigId(configId);
            shard.setBindName(bindName);
            shard.setDatasetId(datasetId);
            shard.setShardIndex(shardIndex);
            shard.setShardType("PAGE");
            shard.setPageNum(pageNum);
            shard.setPageSize(pageSize);
            shard.setStartIndex(startIndex);
            shard.setEndIndex(endIndex);
            shard.setRowCount(rows != null ? rows.size() : 0);

            String columnsJson = JSON.toJSONString(columns);
            shard.setColumnsJson(columnsJson);

            Map<String, Object> shardData = new LinkedHashMap<>();
            shardData.put("columns", columns);
            shardData.put("rows", rows);
            String dataJson = JSON.toJSONString(shardData);
            shard.setDataJson(dataJson);
            shard.setDataSize((long) dataJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

            shard.setStorageEngine("MYSQL");
            shard.setStatus(1);
            shard.setCreateTime(LocalDateTime.now());
            shardMapper.insert(shard);

            logger.debug("存储快照分片成功: snapshotId={}, bindName={}, shardIndex={}, rows={}",
                    snapshotId, bindName, shardIndex, rows != null ? rows.size() : 0);
        } catch (Exception e) {
            logger.error("存储快照分片失败: snapshotId={}, bindName={}, shardIndex={}",
                    snapshotId, bindName, shardIndex, e);
            throw new RuntimeException("存储快照分片失败: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadShard(Long snapshotId, String bindName, int shardIndex) {
        ReportSnapshotShard shard = shardMapper.selectBySnapshotAndShardIndex(snapshotId, bindName, shardIndex);
        if (shard == null || shard.getDataJson() == null) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> data = JSON.parseObject(shard.getDataJson(), Map.class);
            return (List<Map<String, Object>>) data.get("rows");
        } catch (Exception e) {
            logger.error("加载快照分片失败: snapshotId={}, bindName={}, shardIndex={}",
                    snapshotId, bindName, shardIndex, e);
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadPage(Long snapshotId, String bindName, int pageNum, int pageSize) {
        List<ReportSnapshotShard> shards = shardMapper.selectBySnapshotAndBindName(snapshotId, bindName);
        if (shards == null || shards.isEmpty()) {
            return Collections.emptyList();
        }

        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = startIndex + pageSize;

        List<Map<String, Object>> result = new ArrayList<>();
        int currentPos = 0;

        for (ReportSnapshotShard shard : shards) {
            if (currentPos >= endIndex) break;

            int shardRowCount = shard.getRowCount() != null ? shard.getRowCount() : 0;
            int shardStart = currentPos;
            int shardEnd = currentPos + shardRowCount;

            if (shardEnd <= startIndex) {
                currentPos = shardEnd;
                continue;
            }

            try {
                Map<String, Object> data = JSON.parseObject(shard.getDataJson(), Map.class);
                List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
                if (rows == null) {
                    currentPos = shardEnd;
                    continue;
                }

                int fromIdx = Math.max(0, startIndex - currentPos);
                int toIdx = Math.min(rows.size(), endIndex - currentPos);

                if (fromIdx < toIdx) {
                    result.addAll(rows.subList(fromIdx, toIdx));
                }
                currentPos = shardEnd;
            } catch (Exception e) {
                logger.error("读取分片数据失败: shardId={}", shard.getId(), e);
                currentPos = shardEnd;
            }
        }

        return result;
    }

    @Override
    public long getTotalRows(Long snapshotId, String bindName) {
        List<ReportSnapshotShard> shards = shardMapper.selectBySnapshotAndBindName(snapshotId, bindName);
        if (shards == null || shards.isEmpty()) {
            return 0;
        }
        return shards.stream()
                .mapToLong(s -> s.getRowCount() != null ? s.getRowCount() : 0)
                .sum();
    }

    @Override
    public int getTotalShards(Long snapshotId, String bindName) {
        return shardMapper.countShardsBySnapshotAndBindName(snapshotId, bindName);
    }

    @Override
    public List<String> listBindNames(Long snapshotId) {
        return shardMapper.selectDistinctBindNames(snapshotId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getColumns(Long snapshotId, String bindName) {
        List<ReportSnapshotShard> shards = shardMapper.selectBySnapshotAndBindName(snapshotId, bindName);
        if (shards == null || shards.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ReportSnapshotShard first = shards.get(0);
            if (first.getColumnsJson() != null) {
                return JSON.parseObject(first.getColumnsJson(),
                        new com.alibaba.fastjson.TypeReference<List<Map<String, Object>>>() {});
            }
            Map<String, Object> data = JSON.parseObject(first.getDataJson(), Map.class);
            return (List<Map<String, Object>>) data.get("columns");
        } catch (Exception e) {
            logger.error("获取列信息失败: snapshotId={}, bindName={}", snapshotId, bindName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteSnapshotData(Long snapshotId) {
        shardMapper.deleteBySnapshotId(snapshotId);
        logger.info("删除快照分片数据: snapshotId={}", snapshotId);
    }

    @Override
    public Map<String, Object> getStorageInfo(Long snapshotId) {
        Map<String, Object> info = new LinkedHashMap<>();
        List<ReportSnapshotShard> shards = shardMapper.selectBySnapshotId(snapshotId);

        long totalRows = 0;
        long totalSize = 0;
        Set<String> bindNames = new LinkedHashSet<>();

        for (ReportSnapshotShard shard : shards) {
            totalRows += shard.getRowCount() != null ? shard.getRowCount() : 0;
            totalSize += shard.getDataSize() != null ? shard.getDataSize() : 0;
            if (shard.getBindName() != null) {
                bindNames.add(shard.getBindName());
            }
        }

        info.put("storageType", getStorageType());
        info.put("totalShards", shards.size());
        info.put("totalRows", totalRows);
        info.put("totalSize", totalSize);
        info.put("bindNames", new ArrayList<>(bindNames));
        info.put("shards", shards.size());
        return info;
    }
}
