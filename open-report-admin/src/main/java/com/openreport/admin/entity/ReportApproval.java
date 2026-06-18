package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("report_approval")
public class ReportApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("template_name")
    private String templateName;

    @TableField("snapshot_id")
    private Long snapshotId;

    @TableField("version")
    private Integer version;

    @TableField("approval_type")
    private Integer approvalType;

    @TableField("approval_status")
    private Integer approvalStatus;

    @TableField("submit_by")
    private Long submitBy;

    @TableField("submit_by_name")
    private String submitByName;

    @TableField("submit_time")
    private LocalDateTime submitTime;

    @TableField("submit_remark")
    private String submitRemark;

    @TableField("approve_by")
    private Long approveBy;

    @TableField("approve_by_name")
    private String approveByName;

    @TableField("approve_time")
    private LocalDateTime approveTime;

    @TableField("approve_remark")
    private String approveRemark;
}
