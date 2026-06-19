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
@TableName("report_snapshot_config")
public class ReportSnapshotConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_id")
    private Long reportId;

    @TableField("report_name")
    private String reportName;

    @TableField("enabled")
    private Integer enabled;

    @TableField("cron_expression")
    private String cronExpression;

    @TableField("retention_days")
    private Integer retentionDays;

    @TableField("snapshot_type")
    private String snapshotType;

    @TableField("storage_type")
    private String storageType;

    @TableField("params_json")
    private String paramsJson;

    @TableField("description")
    private String description;

    @TableField("last_snapshot_time")
    private LocalDateTime lastSnapshotTime;

    @TableField("last_snapshot_id")
    private Long lastSnapshotId;

    @TableField("snapshot_count")
    private Integer snapshotCount;

    @TableField("max_snapshots")
    private Integer maxSnapshots;

    @TableField("status")
    private Integer status;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_by_name")
    private String createByName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private Long updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
