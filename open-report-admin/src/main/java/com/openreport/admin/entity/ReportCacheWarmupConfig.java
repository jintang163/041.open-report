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
@TableName("report_cache_warmup_config")
public class ReportCacheWarmupConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_name")
    private String configName;

    @TableField("enabled")
    private Integer enabled;

    @TableField("hot_threshold")
    private Integer hotThreshold;

    @TableField("stats_window_days")
    private Integer statsWindowDays;

    @TableField("max_hot_reports")
    private Integer maxHotReports;

    @TableField("low_peak_start_hour")
    private Integer lowPeakStartHour;

    @TableField("low_peak_end_hour")
    private Integer lowPeakEndHour;

    @TableField("cache_ttl_seconds")
    private Long cacheTtlSeconds;

    @TableField("default_params_json")
    private String defaultParamsJson;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
