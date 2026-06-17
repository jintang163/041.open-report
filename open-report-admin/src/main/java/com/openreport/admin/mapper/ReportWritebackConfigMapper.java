package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportWritebackConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportWritebackConfigMapper extends BaseMapper<ReportWritebackConfig> {

    @Select("SELECT * FROM report_writeback_config WHERE report_id = #{reportId} AND deleted = 0")
    List<ReportWritebackConfig> selectByReportId(@Param("reportId") Long reportId);
}
