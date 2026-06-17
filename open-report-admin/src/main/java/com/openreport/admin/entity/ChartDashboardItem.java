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
@TableName("chart_dashboard_item")
public class ChartDashboardItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dashboard_id")
    private Long dashboardId;

    @TableField("title")
    private String title;

    @TableField("chart_type")
    private String chartType;

    @TableField("dataset_id")
    private Long datasetId;

    @TableField("x_field")
    private String xField;

    @TableField("y_fields")
    private String yFields;

    @TableField("linkage_field")
    private String linkageField;

    @TableField("linkage_target_id")
    private Long linkageTargetId;

    @TableField("position_x")
    private Integer positionX;

    @TableField("position_y")
    private Integer positionY;

    @TableField("width")
    private Integer width;

    @TableField("height")
    private Integer height;

    @TableField("chart_config")
    private String chartConfig;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
