package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportWritebackField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportWritebackFieldMapper extends BaseMapper<ReportWritebackField> {

    @Select("SELECT * FROM report_writeback_field WHERE config_id = #{configId} AND deleted = 0")
    List<ReportWritebackField> selectByConfigId(@Param("configId") Long configId);

    @Select("DELETE FROM report_writeback_field WHERE config_id = #{configId}")
    int deleteByConfigId(@Param("configId") Long configId);
}
