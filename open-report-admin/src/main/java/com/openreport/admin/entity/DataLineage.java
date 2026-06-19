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
@TableName("data_lineage")
public class DataLineage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_id")
    private Long reportId;

    @TableField("report_name")
    private String reportName;

    @TableField("report_field")
    private String reportField;

    @TableField("report_field_title")
    private String reportFieldTitle;

    @TableField("data_set_id")
    private Long dataSetId;

    @TableField("data_set_name")
    private String dataSetName;

    @TableField("data_set_field")
    private String dataSetField;

    @TableField("bind_name")
    private String bindName;

    @TableField("expression")
    private String expression;

    @TableField("lineage_type")
    private String lineageType;

    @TableField("datasource_id")
    private Long datasourceId;

    @TableField("datasource_name")
    private String datasourceName;

    @TableField("datasource_type")
    private String datasourceType;

    @TableField("database_name")
    private String databaseName;

    @TableField("schema_name")
    private String schemaName;

    @TableField("table_name")
    private String tableName;

    @TableField("column_name")
    private String columnName;

    @TableField("source_tables")
    private String sourceTables;

    @TableField("source_columns")
    private String sourceColumns;

    @TableField("sql_text")
    private String sqlText;

    @TableField("lineage_hash")
    private String lineageHash;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
