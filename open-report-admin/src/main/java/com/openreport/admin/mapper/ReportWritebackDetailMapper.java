package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportWritebackDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportWritebackDetailMapper extends BaseMapper<ReportWritebackDetail> {

    @Select("SELECT * FROM report_writeback_detail WHERE history_id = #{historyId} ORDER BY row_index")
    List<ReportWritebackDetail> selectByHistoryId(@Param("historyId") Long historyId);
}
