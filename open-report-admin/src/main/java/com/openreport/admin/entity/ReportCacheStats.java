package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("report_cache_stats")
public class ReportCacheStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("template_name")
    private String templateName;

    @TableField("stats_date")
    private LocalDate statsDate;

    @TableField("total_requests")
    private Long totalRequests;

    @TableField("cache_hits")
    private Long cacheHits;

    @TableField("cache_misses")
    private Long cacheMisses;

    @TableField("warmup_count")
    private Long warmupCount;

    @TableField("avg_response_time_ms")
    private Long avgResponseTimeMs;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
