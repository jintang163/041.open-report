package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportApproval;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateSnapshot;
import com.openreport.admin.enums.ApprovalStatusEnum;
import com.openreport.admin.enums.ApprovalTypeEnum;
import com.openreport.admin.mapper.ReportApprovalMapper;
import com.openreport.admin.mapper.ReportTemplateMapper;
import com.openreport.admin.mapper.ReportTemplateSnapshotMapper;
import com.openreport.admin.service.ReportApprovalService;
import com.openreport.admin.service.ReportTemplateSnapshotService;
import com.openreport.common.enums.ReportStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportApprovalServiceImpl extends ServiceImpl<ReportApprovalMapper, ReportApproval>
        implements ReportApprovalService {

    @Autowired
    private ReportTemplateMapper reportTemplateMapper;

    @Autowired
    private ReportTemplateSnapshotService snapshotService;

    @Autowired
    private ReportTemplateSnapshotMapper snapshotMapper;

    @Override
    public List<ReportApproval> listByTemplateId(Long templateId) {
        return baseMapper.listByTemplateId(templateId);
    }

    @Override
    public Page<ReportApproval> pageByStatus(Integer pageNum, Integer pageSize, Integer status) {
        Page<ReportApproval> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageByStatus(page, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportApproval submitApproval(Long templateId, Integer approvalType, Long userId, String userName, String remark) {
        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        ReportTemplateSnapshot snapshot = snapshotService.createSnapshot(
                templateId, userId, userName,
                ApprovalTypeEnum.PUBLISH.getCode().equals(approvalType) ? "提交发布审批" : "提交审批"
        );

        ReportApproval approval = new ReportApproval();
        approval.setTemplateId(templateId);
        approval.setTemplateName(template.getTemplateName());
        approval.setSnapshotId(snapshot.getId());
        approval.setVersion(snapshot.getVersion());
        approval.setApprovalType(approvalType);
        approval.setApprovalStatus(ApprovalStatusEnum.PENDING.getCode());
        approval.setSubmitBy(userId);
        approval.setSubmitByName(userName);
        approval.setSubmitTime(LocalDateTime.now());
        approval.setSubmitRemark(remark);

        baseMapper.insert(approval);

        template.setStatus(ReportStatusEnum.PENDING_APPROVAL.getCode());
        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateMapper.updateById(template);

        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportApproval approve(Long approvalId, Long userId, String userName, String remark, boolean pass) {
        ReportApproval approval = baseMapper.selectById(approvalId);
        if (approval == null) {
            throw new RuntimeException("审批记录不存在");
        }

        if (!ApprovalStatusEnum.PENDING.getCode().equals(approval.getApprovalStatus())) {
            throw new RuntimeException("该审批已处理");
        }

        ReportTemplate template = reportTemplateMapper.selectById(approval.getTemplateId());
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        approval.setApproveBy(userId);
        approval.setApproveByName(userName);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(remark);

        if (pass) {
            approval.setApprovalStatus(ApprovalStatusEnum.APPROVED.getCode());
            if (ApprovalTypeEnum.PUBLISH.getCode().equals(approval.getApprovalType())) {
                template.setStatus(ReportStatusEnum.PUBLISHED.getCode());

                if (approval.getSnapshotId() != null) {
                    ReportTemplateSnapshot snapshot = snapshotMapper.selectById(approval.getSnapshotId());
                    if (snapshot != null) {
                        snapshot.setStatus(ReportStatusEnum.PUBLISHED.getCode());
                        snapshotMapper.updateById(snapshot);
                    }
                }
            } else if (ApprovalTypeEnum.OFFLINE.getCode().equals(approval.getApprovalType())) {
                template.setStatus(ReportStatusEnum.OFFLINE.getCode());
            }
        } else {
            approval.setApprovalStatus(ApprovalStatusEnum.REJECTED.getCode());
            template.setStatus(ReportStatusEnum.REJECTED.getCode());
        }

        baseMapper.updateById(approval);

        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateMapper.updateById(template);

        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportApproval cancelApproval(Long approvalId, Long userId) {
        ReportApproval approval = baseMapper.selectById(approvalId);
        if (approval == null) {
            throw new RuntimeException("审批记录不存在");
        }

        if (!ApprovalStatusEnum.PENDING.getCode().equals(approval.getApprovalStatus())) {
            throw new RuntimeException("该审批已处理，无法撤销");
        }

        if (!approval.getSubmitBy().equals(userId)) {
            throw new RuntimeException("只能撤销自己提交的审批");
        }

        approval.setApprovalStatus(ApprovalStatusEnum.CANCELLED.getCode());
        baseMapper.updateById(approval);

        ReportTemplate template = reportTemplateMapper.selectById(approval.getTemplateId());
        if (template != null) {
            template.setStatus(ReportStatusEnum.DRAFT.getCode());
            template.setUpdateBy(userId);
            template.setUpdateTime(LocalDateTime.now());
            reportTemplateMapper.updateById(template);
        }

        return approval;
    }
}
