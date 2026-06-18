package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportApproval;

import java.util.List;

public interface ReportApprovalService extends IService<ReportApproval> {

    List<ReportApproval> listByTemplateId(Long templateId);

    Page<ReportApproval> pageByStatus(Integer pageNum, Integer pageSize, Integer status);

    ReportApproval submitApproval(Long templateId, Integer approvalType, Long userId, String userName, String remark);

    ReportApproval approve(Long approvalId, Long userId, String userName, String remark, boolean pass);

    ReportApproval cancelApproval(Long approvalId, Long userId);
}
