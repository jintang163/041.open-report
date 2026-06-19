package com.openreport.admin.service.snapshot;

import java.util.List;
import java.util.Map;

public interface SnapshotStorageStrategy {

    String getStorageType();

    boolean isEnabled();

    void storeShard(Long snapshotId, Long reportId, Long configId,
                    String bindName, Long datasetId,
                    int shardIndex, int pageNum, int pageSize,
                    long startIndex, long endIndex,
                    List<Map<String, Object>> rows,
                    List<Map<String, Object>> columns);

    List<Map<String, Object>> loadShard(Long snapshotId, String bindName, int shardIndex);

    List<Map<String, Object>> loadPage(Long snapshotId, String bindName, int pageNum, int pageSize);

    long getTotalRows(Long snapshotId, String bindName);

    int getTotalShards(Long snapshotId, String bindName);

    List<String> listBindNames(Long snapshotId);

    List<Map<String, Object>> getColumns(Long snapshotId, String bindName);

    void deleteSnapshotData(Long snapshotId);

    Map<String, Object> getStorageInfo(Long snapshotId);
}
