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
@TableName("report_data_snapshot")
public class ReportDataSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_id")
    private Long reportId;

    @TableField("report_name")
    private String reportName;

    @TableField("config_id")
    private Long configId;

    @TableField("snapshot_name")
    private String snapshotName;

    @TableField("snapshot_type")
    private String snapshotType;

    @TableField("storage_type")
    private String storageType;

    @TableField("data_version")
    private String dataVersion;

    @TableField("params_json")
    private String paramsJson;

    @TableField("data_json")
    private String dataJson;

    @TableField("data_size")
    private Long dataSize;

    @TableField("row_count")
    private Long rowCount;

    @TableField("table_count")
    private Integer tableCount;

    @TableField("execute_time")
    private Long executeTime;

    @TableField("data_hash")
    private String dataHash;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_by_name")
    private String createByName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
