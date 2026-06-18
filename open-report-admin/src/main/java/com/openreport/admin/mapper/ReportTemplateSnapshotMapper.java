package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportTemplateSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportTemplateSnapshotMapper extends BaseMapper<ReportTemplateSnapshot> {

    @Select("SELECT MAX(version) FROM report_template_snapshot WHERE template_id = #{templateId}")
    Integer getMaxVersion(@Param("templateId") Long templateId);

    @Select("SELECT * FROM report_template_snapshot WHERE template_id = #{templateId} ORDER BY version DESC")
    List<ReportTemplateSnapshot> listByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM report_template_snapshot WHERE template_id = #{templateId} AND version = #{version}")
    ReportTemplateSnapshot getByVersion(@Param("templateId") Long templateId, @Param("version") Integer version);
}
