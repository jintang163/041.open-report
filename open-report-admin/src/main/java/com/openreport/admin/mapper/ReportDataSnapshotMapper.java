package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportDataSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReportDataSnapshotMapper extends BaseMapper<ReportDataSnapshot> {

    @Select("SELECT * FROM report_data_snapshot WHERE report_id = #{reportId} AND deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<ReportDataSnapshot> selectByReportId(@Param("reportId") Long reportId, @Param("limit") Integer limit);

    @Select("SELECT * FROM report_data_snapshot WHERE config_id = #{configId} AND deleted = 0 ORDER BY create_time DESC")
    List<ReportDataSnapshot> selectByConfigId(@Param("configId") Long configId);

    @Select("SELECT * FROM report_data_snapshot WHERE expire_time < #{now} AND deleted = 0 AND status = 1")
    List<ReportDataSnapshot> selectExpiredSnapshots(@Param("now") LocalDateTime now);

    @Select("SELECT * FROM report_data_snapshot WHERE report_id = #{reportId} AND deleted = 0 ORDER BY create_time DESC LIMIT 1")
    ReportDataSnapshot selectLatestByReportId(@Param("reportId") Long reportId);

    @Delete("DELETE FROM report_data_snapshot WHERE expire_time < #{now} AND deleted = 0")
    int deleteExpiredSnapshots(@Param("now") LocalDateTime now);

    @Select("SELECT * FROM report_data_snapshot WHERE report_id = #{reportId} AND deleted = 0 " +
            "AND create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<ReportDataSnapshot> selectByReportIdAndTimeRange(
            @Param("reportId") Long reportId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
