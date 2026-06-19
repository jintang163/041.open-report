package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportDataSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportDataSnapshotService extends IService<ReportDataSnapshot> {

    List<ReportDataSnapshot> listByReportId(Long reportId, Integer limit);

    List<ReportDataSnapshot> listByConfigId(Long configId);

    ReportDataSnapshot getLatestByReportId(Long reportId);

    List<ReportDataSnapshot> listExpiredSnapshots();

    int deleteExpiredSnapshots();

    List<ReportDataSnapshot> listByReportIdAndTimeRange(Long reportId, LocalDateTime startTime, LocalDateTime endTime);

    Map<String, Object> loadSnapshotData(Long snapshotId);

    Map<String, Object> compareSnapshots(Long baseSnapshotId, Long targetSnapshotId);

    Map<String, Object> compareSnapshotWithRealtime(Long snapshotId, Map<String, Object> params);

    Map<String, Object> getSnapshotDataPage(Long snapshotId, String bindName, Integer pageNum, Integer pageSize);

    Map<String, Object> getSnapshotStorageInfo(Long snapshotId);

    List<String> getSnapshotBindNames(Long snapshotId);
}
