package com.openreport.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.dto.writeback.DataSubmitRequest;
import com.openreport.admin.dto.writeback.DataSubmitResult;
import com.openreport.admin.entity.ReportWritebackDetail;
import com.openreport.admin.entity.ReportWritebackHistory;

import java.util.List;

public interface ReportWritebackHistoryService extends IService<ReportWritebackHistory> {

    DataSubmitResult submitData(DataSubmitRequest request, Long userId);

    IPage<ReportWritebackHistory> getHistoryPage(Integer pageNum, Integer pageSize, Long reportId);

    ReportWritebackHistory getHistoryDetail(Long id);

    List<ReportWritebackDetail> getHistoryDetails(Long historyId);
}
