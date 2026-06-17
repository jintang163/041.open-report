package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.ReportWritebackHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportWritebackHistoryMapper extends BaseMapper<ReportWritebackHistory> {

    @Select("SELECT * FROM report_writeback_history WHERE report_id = #{reportId} AND deleted = 0 ORDER BY create_time DESC")
    IPage<ReportWritebackHistory> selectByReportId(Page<ReportWritebackHistory> page, @Param("reportId") Long reportId);
}
