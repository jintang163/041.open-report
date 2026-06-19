package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("report_function_version")
public class ReportFunctionVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("func_id")
    private Long funcId;

    @TableField("version")
    private Integer version;

    @TableField("script_content")
    private String scriptContent;

    @TableField("script_type")
    private String scriptType;

    @TableField("change_log")
    private String changeLog;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private LocalDateTime createTime;
}
