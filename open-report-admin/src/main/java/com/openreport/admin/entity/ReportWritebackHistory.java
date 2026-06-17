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
@TableName("report_writeback_history")
public class ReportWritebackHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_id")
    private Long reportId;

    @TableField("config_id")
    private Long configId;

    @TableField("batch_no")
    private String batchNo;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("status")
    private String status;

    @TableField("execute_time")
    private Long executeTime;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("params")
    private String params;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private Long createBy;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private java.util.List<ReportWritebackDetail> details;
}
