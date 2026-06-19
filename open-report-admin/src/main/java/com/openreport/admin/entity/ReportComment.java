package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("report_comment")
public class ReportComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("template_name")
    private String templateName;

    @TableField("snapshot_version")
    private Integer snapshotVersion;

    @TableField("cell_ref")
    private String cellRef;

    @TableField("chart_id")
    private String chartId;

    @TableField("content")
    private String content;

    @TableField("parent_id")
    private Long parentId;

    @TableField("reply_to_user_id")
    private Long replyToUserId;

    @TableField("reply_to_user_name")
    private String replyToUserName;

    @TableField("mention_user_ids")
    private String mentionUserIds;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("reply_count")
    private Integer replyCount;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_by_name")
    private String createByName;

    @TableField("create_by_avatar")
    private String createByAvatar;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Boolean liked;

    @TableField(exist = false)
    private java.util.List<ReportComment> replies;
}
