package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportCacheStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReportCacheStatsMapper extends BaseMapper<ReportCacheStats> {

    @Select("SELECT * FROM report_cache_stats WHERE template_id = #{templateId} AND stats_date = #{date} LIMIT 1")
    ReportCacheStats selectByTemplateAndDate(
            @Param("templateId") Long templateId,
            @Param("date") LocalDate date);

    @Insert("INSERT INTO report_cache_stats (template_id, template_name, stats_date, total_requests, " +
            "cache_hits, cache_misses, warmup_count, avg_response_time_ms, create_time, update_time) " +
            "VALUES (#{templateId}, #{templateName}, #{statsDate}, #{totalRequests}, " +
            "#{cacheHits}, #{cacheMisses}, #{warmupCount}, #{avgResponseTimeMs}, #{createTime}, #{updateTime})")
    int insertStats(ReportCacheStats stats);

    @Update("UPDATE report_cache_stats SET total_requests = #{totalRequests}, " +
            "cache_hits = #{cacheHits}, cache_misses = #{cacheMisses}, " +
            "warmup_count = #{warmupCount}, avg_response_time_ms = #{avgResponseTimeMs}, " +
            "update_time = #{updateTime} WHERE id = #{id}")
    int updateStats(ReportCacheStats stats);

    @Select("SELECT * FROM report_cache_stats WHERE stats_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY stats_date, total_requests DESC")
    List<ReportCacheStats> selectByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
