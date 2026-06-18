package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.ReportApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportApprovalMapper extends BaseMapper<ReportApproval> {

    @Select("SELECT * FROM report_approval WHERE template_id = #{templateId} ORDER BY create_time DESC")
    List<ReportApproval> listByTemplateId(@Param("templateId") Long templateId);

    @Select("<script>" +
            "SELECT * FROM report_approval " +
            "<where>" +
            "  <if test='status != null'>AND approval_status = #{status}</if>" +
            "</where>" +
            "ORDER BY submit_time DESC" +
            "</script>")
    Page<ReportApproval> pageByStatus(Page<ReportApproval> page, @Param("status") Integer status);
}
