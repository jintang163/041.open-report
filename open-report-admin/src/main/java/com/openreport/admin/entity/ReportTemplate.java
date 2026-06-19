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
@TableName("report_template")
public class ReportTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_name")
    private String templateName;

    @TableField("template_code")
    private String templateCode;

    @TableField("template_type")
    private Integer templateType;

    @TableField("template_json")
    private String templateJson;

    @TableField("data_set_bind")
    private String dataSetBind;

    @TableField("param_config")
    private String paramConfig;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;

    @TableField("share_enabled")
    private Integer shareEnabled;

    @TableField("share_token")
    private String shareToken;

    @TableField("share_expire_time")
    private LocalDateTime shareExpireTime;

    @TableField("share_password")
    private String sharePassword;

    @TableField("share_view_count")
    private Long shareViewCount;

    @TableField("create_by")
    private Long createBy;

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
