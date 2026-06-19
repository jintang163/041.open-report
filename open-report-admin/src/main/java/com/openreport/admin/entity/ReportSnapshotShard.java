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
@TableName("report_snapshot_shard")
public class ReportSnapshotShard implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("snapshot_id")
    private Long snapshotId;

    @TableField("report_id")
    private Long reportId;

    @TableField("config_id")
    private Long configId;

    @TableField("bind_name")
    private String bindName;

    @TableField("dataset_id")
    private Long datasetId;

    @TableField("shard_index")
    private Integer shardIndex;

    @TableField("shard_type")
    private String shardType;

    @TableField("page_num")
    private Integer pageNum;

    @TableField("page_size")
    private Integer pageSize;

    @TableField("start_index")
    private Long startIndex;

    @TableField("end_index")
    private Long endIndex;

    @TableField("row_count")
    private Integer rowCount;

    @TableField("columns_json")
    private String columnsJson;

    @TableField("data_json")
    private String dataJson;

    @TableField("data_size")
    private Long dataSize;

    @TableField("storage_engine")
    private String storageEngine;

    @TableField("clickhouse_table")
    private String clickhouseTable;

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
