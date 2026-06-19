package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportCommentLikeMapper extends BaseMapper<ReportCommentLike> {

    @Select("SELECT COUNT(*) FROM report_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int countByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
