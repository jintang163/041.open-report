package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportSnapshotConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportSnapshotConfigMapper extends BaseMapper<ReportSnapshotConfig> {

    @Select("SELECT * FROM report_snapshot_config WHERE enabled = 1 AND status = 1 AND deleted = 0")
    List<ReportSnapshotConfig> selectEnabledConfigs();

    @Select("SELECT * FROM report_snapshot_config WHERE report_id = #{reportId} AND deleted = 0 ORDER BY create_time DESC")
    List<ReportSnapshotConfig> selectByReportId(@Param("reportId") Long reportId);
}
