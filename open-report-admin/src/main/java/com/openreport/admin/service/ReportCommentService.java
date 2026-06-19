package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportComment;

import java.util.List;

public interface ReportCommentService extends IService<ReportComment> {

    List<ReportComment> listByTemplateId(Long templateId, Long currentUserId);

    List<ReportComment> listByTemplateIdAndVersion(Long templateId, Integer snapshotVersion, Long currentUserId);

    List<ReportComment> listByCellRef(Long templateId, String cellRef, Long currentUserId);

    List<ReportComment> listByChartId(Long templateId, String chartId, Long currentUserId);

    List<String> getCellRefsWithComments(Long templateId);

    List<String> getChartIdsWithComments(Long templateId);

    int countByTemplateId(Long templateId);

    ReportComment addComment(ReportComment comment, Long userId, String userName);

    ReportComment addReply(Long parentId, ReportComment reply, Long userId, String userName);

    void deleteComment(Long commentId, Long userId);

    boolean toggleLike(Long commentId, Long userId, String userName);

    void sendMentionNotifications(ReportComment comment);
}
