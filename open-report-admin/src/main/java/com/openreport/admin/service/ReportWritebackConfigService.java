package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.dto.writeback.WritebackConfigDTO;
import com.openreport.admin.entity.ReportWritebackConfig;

import java.util.List;

public interface ReportWritebackConfigService extends IService<ReportWritebackConfig> {

    List<ReportWritebackConfig> getByReportId(Long reportId);

    ReportWritebackConfig getDetailById(Long id);

    void saveConfig(WritebackConfigDTO dto);

    void updateConfig(WritebackConfigDTO dto);

    void deleteConfig(Long id);
}
