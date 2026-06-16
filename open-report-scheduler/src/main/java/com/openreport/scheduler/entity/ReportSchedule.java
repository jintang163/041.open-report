package com.openreport.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.openreport.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_schedule")
public class ReportSchedule extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("report_id")
    private Long reportId;

    @TableField("cron_expression")
    private String cronExpression;

    @TableField("params")
    private String params;

    @TableField("output_type")
    private String outputType;

    @TableField("email_list")
    private String emailList;

    @TableField("status")
    private Integer status;

    @TableField("last_execute_time")
    private LocalDateTime lastExecuteTime;

    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;
}
