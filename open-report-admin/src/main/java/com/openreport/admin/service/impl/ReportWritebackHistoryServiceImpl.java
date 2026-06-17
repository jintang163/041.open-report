package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.dto.writeback.DataSubmitRequest;
import com.openreport.admin.dto.writeback.DataSubmitResult;
import com.openreport.admin.engine.writeback.WritebackEngine;
import com.openreport.admin.entity.ReportWritebackDetail;
import com.openreport.admin.entity.ReportWritebackHistory;
import com.openreport.admin.mapper.ReportWritebackDetailMapper;
import com.openreport.admin.mapper.ReportWritebackHistoryMapper;
import com.openreport.admin.service.ReportWritebackHistoryService;
import com.openreport.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportWritebackHistoryServiceImpl extends ServiceImpl<ReportWritebackHistoryMapper, ReportWritebackHistory> implements ReportWritebackHistoryService {

    @Autowired
    private WritebackEngine writebackEngine;

    @Autowired
    private ReportWritebackDetailMapper detailMapper;

    @Override
    public DataSubmitResult submitData(DataSubmitRequest request, Long userId) {
        return writebackEngine.executeSubmit(request, userId);
    }

    @Override
    public IPage<ReportWritebackHistory> getHistoryPage(Integer pageNum, Integer pageSize, Long reportId) {
        if (reportId == null) {
            throw new BusinessException("报表ID不能为空");
        }
        Page<ReportWritebackHistory> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectByReportId(page, reportId);
    }

    @Override
    public ReportWritebackHistory getHistoryDetail(Long id) {
        ReportWritebackHistory history = getById(id);
        if (history == null) {
            throw new BusinessException("提交历史不存在");
        }
        history.setDetails(detailMapper.selectByHistoryId(id));
        return history;
    }

    @Override
    public List<ReportWritebackDetail> getHistoryDetails(Long historyId) {
        return detailMapper.selectByHistoryId(historyId);
    }
}
