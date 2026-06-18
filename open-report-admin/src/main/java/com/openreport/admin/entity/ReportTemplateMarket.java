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
@TableName("report_template_market")
public class ReportTemplateMarket implements Serializable {

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

    @TableField("cover_image")
    private String coverImage;

    @TableField("visibility")
    private Integer visibility;

    @TableField("category")
    private String category;

    @TableField("tags")
    private String tags;

    @TableField("version")
    private String version;

    @TableField("install_count")
    private Integer installCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("status")
    private Integer status;

    @TableField("author_id")
    private Long authorId;

    @TableField("author_name")
    private String authorName;

    @TableField("source_template_id")
    private Long sourceTemplateId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
