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
@TableName("report_writeback_config")
public class ReportWritebackConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_id")
    private Long reportId;

    @TableField("data_source_id")
    private Long dataSourceId;

    @TableField("table_name")
    private String tableName;

    @TableField("primary_key_field")
    private String primaryKeyField;

    @TableField("primary_key_column")
    private String primaryKeyColumn;

    @TableField("version_field")
    private String versionField;

    @TableField("logic_delete_field")
    private String logicDeleteField;

    @TableField("logic_delete_value")
    private String logicDeleteValue;

    @TableField("logic_not_delete_value")
    private String logicNotDeleteValue;

    @TableField("batch_support")
    private Integer batchSupport;

    @TableField("transaction_enable")
    private Integer transactionEnable;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private java.util.List<ReportWritebackField> fields;
}
