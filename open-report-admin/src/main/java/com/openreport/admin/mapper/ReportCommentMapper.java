package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReportCommentMapper extends BaseMapper<ReportComment> {

    @Select("SELECT * FROM report_comment WHERE template_id = #{templateId} AND deleted = 0 ORDER BY create_time DESC")
    List<ReportComment> listByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM report_comment WHERE template_id = #{templateId} AND snapshot_version = #{snapshotVersion} AND deleted = 0 ORDER BY create_time DESC")
    List<ReportComment> listByTemplateIdAndVersion(@Param("templateId") Long templateId, @Param("snapshotVersion") Integer snapshotVersion);

    @Select("SELECT * FROM report_comment WHERE template_id = #{templateId} AND cell_ref = #{cellRef} AND deleted = 0 ORDER BY create_time DESC")
    List<ReportComment> listByCellRef(@Param("templateId") Long templateId, @Param("cellRef") String cellRef);

    @Select("SELECT * FROM report_comment WHERE template_id = #{templateId} AND chart_id = #{chartId} AND deleted = 0 ORDER BY create_time DESC")
    List<ReportComment> listByChartId(@Param("templateId") Long templateId, @Param("chartId") String chartId);

    @Select("SELECT * FROM report_comment WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY create_time ASC")
    List<ReportComment> listReplies(@Param("parentId") Long parentId);

    @Select("SELECT DISTINCT cell_ref FROM report_comment WHERE template_id = #{templateId} AND cell_ref IS NOT NULL AND deleted = 0")
    List<String> getCellRefsWithComments(@Param("templateId") Long templateId);

    @Select("SELECT DISTINCT chart_id FROM report_comment WHERE template_id = #{templateId} AND chart_id IS NOT NULL AND deleted = 0")
    List<String> getChartIdsWithComments(@Param("templateId") Long templateId);

    @Select("SELECT DISTINCT cell_ref FROM report_comment WHERE template_id = #{templateId} AND snapshot_version = #{snapshotVersion} AND cell_ref IS NOT NULL AND deleted = 0")
    List<String> getCellRefsWithCommentsByVersion(@Param("templateId") Long templateId, @Param("snapshotVersion") Integer snapshotVersion);

    @Select("SELECT DISTINCT chart_id FROM report_comment WHERE template_id = #{templateId} AND snapshot_version = #{snapshotVersion} AND chart_id IS NOT NULL AND deleted = 0")
    List<String> getChartIdsWithCommentsByVersion(@Param("templateId") Long templateId, @Param("snapshotVersion") Integer snapshotVersion);

    @Update("UPDATE report_comment SET like_count = like_count + #{delta} WHERE id = #{id} AND deleted = 0")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE report_comment SET reply_count = reply_count + #{delta} WHERE id = #{id} AND deleted = 0")
    int updateReplyCount(@Param("id") Long id, @Param("delta") int delta);

    @Select("SELECT COUNT(*) FROM report_comment WHERE template_id = #{templateId} AND deleted = 0")
    int countByTemplateId(@Param("templateId") Long templateId);
}
