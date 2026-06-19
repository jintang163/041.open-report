package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportAccessLogMapper extends BaseMapper<ReportAccessLog> {

    @Select("SELECT template_id, template_name, COUNT(*) as access_count " +
            "FROM report_access_log " +
            "WHERE access_date >= #{startDate} AND access_date <= #{endDate} " +
            "GROUP BY template_id, template_name " +
            "ORDER BY access_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopHotReports(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") Integer limit);

    @Select("SELECT template_id, COUNT(*) as access_count " +
            "FROM report_access_log " +
            "WHERE access_date >= #{startDate} AND access_date <= #{endDate} " +
            "GROUP BY template_id " +
            "HAVING access_count >= #{threshold} " +
            "ORDER BY access_count DESC")
    List<Map<String, Object>> selectHotReportsWithThreshold(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    @Select("SELECT template_id, params_hash, COUNT(*) as access_count " +
            "FROM report_access_log " +
            "WHERE access_date >= #{startDate} AND access_date <= #{endDate} " +
            "  AND params_hash IS NOT NULL " +
            "GROUP BY template_id, params_hash " +
            "HAVING access_count >= #{threshold} " +
            "ORDER BY access_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectHotReportParamCombos(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold,
            @Param("limit") Integer limit);

    @Select("SELECT DATE(create_time) as stat_date, " +
            "COUNT(*) as total_requests, " +
            "SUM(CASE WHEN hit_cache = 1 THEN 1 ELSE 0 END) as cache_hits, " +
            "SUM(CASE WHEN hit_cache = 0 THEN 1 ELSE 0 END) as cache_misses, " +
            "AVG(response_time_ms) as avg_response_time_ms " +
            "FROM report_access_log " +
            "WHERE access_date >= #{startDate} AND access_date <= #{endDate} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY stat_date")
    List<Map<String, Object>> selectOverallStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("SELECT template_id, template_name, " +
            "COUNT(*) as total_requests, " +
            "SUM(CASE WHEN hit_cache = 1 THEN 1 ELSE 0 END) as cache_hits, " +
            "SUM(CASE WHEN hit_cache = 0 THEN 1 ELSE 0 END) as cache_misses, " +
            "AVG(response_time_ms) as avg_response_time_ms " +
            "FROM report_access_log " +
            "WHERE access_date = #{date} " +
            "GROUP BY template_id, template_name " +
            "ORDER BY total_requests DESC")
    List<Map<String, Object>> selectDailyStatsByTemplate(@Param("date") LocalDate date);
}
