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
@TableName("report_component")
public class ReportComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("component_name")
    private String componentName;

    @TableField("component_code")
    private String componentCode;

    @TableField("component_type")
    private Integer componentType;

    @TableField("component_json")
    private String componentJson;

    @TableField("description")
    private String description;

    @TableField("icon")
    private String icon;

    @TableField("version")
    private String version;

    @TableField("category")
    private String category;

    @TableField("tags")
    private String tags;

    @TableField("source")
    private Integer source;

    @TableField("download_count")
    private Integer downloadCount;

    @TableField("status")
    private Integer status;

    @TableField("author_id")
    private Long authorId;

    @TableField("author_name")
    private String authorName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
