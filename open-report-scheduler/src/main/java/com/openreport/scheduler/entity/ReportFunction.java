package com.openreport.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_function")
public class ReportFunction {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("func_name")
    private String funcName;

    @TableField("func_label")
    private String funcLabel;

    @TableField("func_category")
    private String funcCategory;

    @TableField("description")
    private String description;

    @TableField("param_config")
    private String paramConfig;

    @TableField("return_type")
    private String returnType;

    @TableField("example")
    private String example;

    @TableField("status")
    private Integer status;

    @TableField("current_version")
    private Integer currentVersion;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private Long updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
