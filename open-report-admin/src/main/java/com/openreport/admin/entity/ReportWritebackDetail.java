package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("report_writeback_detail")
public class ReportWritebackDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("history_id")
    private Long historyId;

    @TableField("row_index")
    private Integer rowIndex;

    @TableField("row_status")
    private String rowStatus;

    @TableField("primary_key_value")
    private String primaryKeyValue;

    @TableField("old_data")
    private String oldData;

    @TableField("new_data")
    private String newData;

    @TableField("status")
    private String status;

    @TableField("execute_sql")
    private String executeSql;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_time")
    private LocalDateTime createTime;
}
