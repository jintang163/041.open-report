package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportSnapshotConfig;

import java.util.List;
import java.util.Map;

public interface ReportSnapshotConfigService extends IService<ReportSnapshotConfig> {

    List<ReportSnapshotConfig> listEnabledConfigs();

    List<ReportSnapshotConfig> listByReportId(Long reportId);

    ReportSnapshotConfig createConfig(ReportSnapshotConfig config);

    ReportSnapshotConfig updateConfig(ReportSnapshotConfig config);

    boolean deleteConfig(Long id);

    boolean toggleEnabled(Long id, Integer enabled);

    ReportSnapshotConfig getByReportId(Long reportId);

    Map<String, Object> createSnapshot(Long configId, Map<String, Object> params);

    boolean cleanupExpiredSnapshots();
}
