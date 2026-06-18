package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.dto.TemplateVersionDiffDTO;
import com.openreport.admin.entity.ReportTemplateSnapshot;

import java.util.List;

public interface ReportTemplateSnapshotService extends IService<ReportTemplateSnapshot> {

    List<ReportTemplateSnapshot> listByTemplateId(Long templateId);

    ReportTemplateSnapshot getByVersion(Long templateId, Integer version);

    ReportTemplateSnapshot createSnapshot(Long templateId, Long userId, String userName, String changeLog);

    ReportTemplateSnapshot rollbackToVersion(Long templateId, Integer version, Long userId, String userName);

    Integer getMaxVersion(Long templateId);

    TemplateVersionDiffDTO compareVersions(Long templateId, Integer baseVersion, Integer targetVersion);

    ReportTemplateSnapshot getLatestPublishedVersion(Long templateId);

    ReportTemplateSnapshot previewPublish(Long templateId);
}
