package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("report_access_log")
public class ReportAccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("template_name")
    private String templateName;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("access_date")
    private LocalDate accessDate;

    @TableField("access_hour")
    private Integer accessHour;

    @TableField("params_hash")
    private String paramsHash;

    @TableField("response_time_ms")
    private Long responseTimeMs;

    @TableField("hit_cache")
    private Integer hitCache;

    @TableField("create_time")
    private LocalDateTime createTime;
}
