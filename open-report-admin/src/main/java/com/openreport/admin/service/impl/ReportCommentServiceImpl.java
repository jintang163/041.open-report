package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportComment;
import com.openreport.admin.entity.ReportCommentLike;
import com.openreport.admin.entity.SysUser;
import com.openreport.admin.mapper.ReportCommentLikeMapper;
import com.openreport.admin.mapper.ReportCommentMapper;
import com.openreport.admin.mapper.SysUserMapper;
import com.openreport.admin.service.ReportCommentService;
import com.openreport.admin.websocket.WebSocketPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportCommentServiceImpl extends ServiceImpl<ReportCommentMapper, ReportComment>
        implements ReportCommentService {

    @Autowired
    private ReportCommentLikeMapper commentLikeMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private WebSocketPushService pushService;

    @Override
    public List<ReportComment> listByTemplateId(Long templateId, Long currentUserId) {
        List<ReportComment> comments = baseMapper.listByTemplateId(templateId);
        fillLikeStatus(comments, currentUserId);
        fillReplies(comments, currentUserId);
        return comments.stream().filter(c -> c.getParentId() == null).collect(Collectors.toList());
    }

    @Override
    public List<ReportComment> listByTemplateIdAndVersion(Long templateId, Integer snapshotVersion, Long currentUserId) {
        List<ReportComment> comments = baseMapper.listByTemplateIdAndVersion(templateId, snapshotVersion);
        fillLikeStatus(comments, currentUserId);
        fillReplies(comments, currentUserId);
        return comments.stream().filter(c -> c.getParentId() == null).collect(Collectors.toList());
    }

    @Override
    public List<ReportComment> listByCellRef(Long templateId, String cellRef, Long currentUserId) {
        List<ReportComment> comments = baseMapper.listByCellRef(templateId, cellRef);
        fillLikeStatus(comments, currentUserId);
        fillReplies(comments, currentUserId);
        return comments.stream().filter(c -> c.getParentId() == null).collect(Collectors.toList());
    }

    @Override
    public List<ReportComment> listByChartId(Long templateId, String chartId, Long currentUserId) {
        List<ReportComment> comments = baseMapper.listByChartId(templateId, chartId);
        fillLikeStatus(comments, currentUserId);
        fillReplies(comments, currentUserId);
        return comments.stream().filter(c -> c.getParentId() == null).collect(Collectors.toList());
    }

    @Override
    public List<String> getCellRefsWithComments(Long templateId) {
        return baseMapper.getCellRefsWithComments(templateId);
    }

    @Override
    public List<String> getChartIdsWithComments(Long templateId) {
        return baseMapper.getChartIdsWithComments(templateId);
    }

    @Override
    public int countByTemplateId(Long templateId) {
        return baseMapper.countByTemplateId(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportComment addComment(ReportComment comment, Long userId, String userName) {
        comment.setCreateBy(userId);
        comment.setCreateByName(userName);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setDeleted(0);

        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            comment.setCreateByAvatar(user.getAvatar());
        }

        baseMapper.insert(comment);

        sendMentionNotifications(comment);

        pushCommentChange(comment.getTemplateId(), "ADD", comment);

        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportComment addReply(Long parentId, ReportComment reply, Long userId, String userName) {
        ReportComment parent = baseMapper.selectById(parentId);
        if (parent == null) {
            throw new RuntimeException("父评论不存在");
        }

        reply.setParentId(parentId);
        reply.setTemplateId(parent.getTemplateId());
        reply.setTemplateName(parent.getTemplateName());
        reply.setSnapshotVersion(parent.getSnapshotVersion());
        reply.setCellRef(parent.getCellRef());
        reply.setChartId(parent.getChartId());
        reply.setCreateBy(userId);
        reply.setCreateByName(userName);
        reply.setLikeCount(0);
        reply.setReplyCount(0);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        reply.setDeleted(0);

        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            reply.setCreateByAvatar(user.getAvatar());
        }

        baseMapper.insert(reply);
        baseMapper.updateReplyCount(parentId, 1);

        sendMentionNotifications(reply);

        pushCommentChange(reply.getTemplateId(), "REPLY", reply);

        return reply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        ReportComment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getCreateBy().equals(userId)) {
            throw new RuntimeException("只能删除自己的评论");
        }

        if (comment.getParentId() != null) {
            baseMapper.updateReplyCount(comment.getParentId(), -1);
        }

        List<ReportComment> replies = baseMapper.listReplies(commentId);
        for (ReportComment reply : replies) {
            reply.setDeleted(1);
            baseMapper.updateById(reply);
        }

        comment.setDeleted(1);
        baseMapper.updateById(comment);

        pushCommentChange(comment.getTemplateId(), "DELETE", comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long commentId, Long userId, String userName) {
        ReportComment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        int existCount = commentLikeMapper.countByCommentIdAndUserId(commentId, userId);
        if (existCount > 0) {
            Map<String, Object> deleteMap = new HashMap<>();
            deleteMap.put("comment_id", commentId);
            deleteMap.put("user_id", userId);
            commentLikeMapper.deleteByMap(deleteMap);
            baseMapper.updateLikeCount(commentId, -1);
            return false;
        } else {
            ReportCommentLike like = new ReportCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setUserName(userName);
            like.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(like);
            baseMapper.updateLikeCount(commentId, 1);
            return true;
        }
    }

    @Override
    public void sendMentionNotifications(ReportComment comment) {
        if (comment.getMentionUserIds() == null || comment.getMentionUserIds().isEmpty()) {
            return;
        }

        String[] userIdStrs = comment.getMentionUserIds().split(",");
        List<Long> mentionUserIds = new ArrayList<>();
        for (String idStr : userIdStrs) {
            try {
                mentionUserIds.add(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID: {}", idStr);
            }
        }

        if (mentionUserIds.isEmpty()) {
            return;
        }

        for (Long mentionUserId : mentionUserIds) {
            SysUser mentionUser = sysUserMapper.selectById(mentionUserId);
            if (mentionUser == null) {
                continue;
            }

            try {
                pushService.pushCommentMention(
                        comment.getTemplateId(),
                        comment.getId(),
                        comment.getCreateByName(),
                        mentionUserId,
                        comment.getContent()
                );
            } catch (Exception e) {
                log.error("推送评论@提及通知失败: userId={}", mentionUserId, e);
            }

            if (mentionUser.getEmail() != null && !mentionUser.getEmail().isEmpty()) {
                try {
                    pushService.sendEmailNotification(
                            mentionUser.getEmail(),
                            String.format("【报表评论】%s 在报表中@了您", comment.getCreateByName()),
                            buildMentionEmailContent(comment, mentionUser.getNickname())
                    );
                } catch (Exception e) {
                    log.error("发送评论@提及邮件失败: email={}", mentionUser.getEmail(), e);
                }
            }
        }
    }

    private void fillLikeStatus(List<ReportComment> comments, Long currentUserId) {
        if (currentUserId == null || comments.isEmpty()) {
            comments.forEach(c -> c.setLiked(false));
            return;
        }
        for (ReportComment comment : comments) {
            int count = commentLikeMapper.countByCommentIdAndUserId(comment.getId(), currentUserId);
            comment.setLiked(count > 0);
        }
    }

    private void fillReplies(List<ReportComment> comments, Long currentUserId) {
        for (ReportComment comment : comments) {
            if (comment.getParentId() == null) {
                List<ReportComment> replies = baseMapper.listReplies(comment.getId());
                fillLikeStatus(replies, currentUserId);
                comment.setReplies(replies);
            }
        }
    }

    private void pushCommentChange(Long templateId, String action, ReportComment comment) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("templateId", templateId);
            payload.put("action", action);
            payload.put("commentId", comment.getId());
            payload.put("cellRef", comment.getCellRef());
            payload.put("chartId", comment.getChartId());

            pushService.pushCommentChange(templateId, action, payload);
        } catch (Exception e) {
            log.error("推送评论变更失败: templateId={}", templateId, e);
        }
    }

    private String buildMentionEmailContent(ReportComment comment, String mentionUserName) {
        return String.format(
                "<p>%s，您好！</p>" +
                "<p><strong>%s</strong> 在报表 <strong>%s</strong> 中@了您：</p>" +
                "<blockquote>%s</blockquote>" +
                "<p>请及时查看并回复。</p>",
                mentionUserName,
                comment.getCreateByName(),
                comment.getTemplateName() != null ? comment.getTemplateName() : "报表",
                comment.getContent()
        );
    }
}
