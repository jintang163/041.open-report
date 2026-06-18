package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("report_template_snapshot")
public class ReportTemplateSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("version")
    private Integer version;

    @TableField("template_name")
    private String templateName;

    @TableField("template_json")
    private String templateJson;

    @TableField("data_set_bind")
    private String dataSetBind;

    @TableField("param_config")
    private String paramConfig;

    @TableField("description")
    private String description;

    @TableField("change_log")
    private String changeLog;

    @TableField("status")
    private Integer status;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_by_name")
    private String createByName;

    @TableField("create_time")
    private LocalDateTime createTime;
}
