package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportSnapshotShard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface ReportSnapshotShardMapper extends BaseMapper<ReportSnapshotShard> {

    @Select("SELECT * FROM report_snapshot_shard WHERE snapshot_id = #{snapshotId} AND deleted = 0 ORDER BY shard_index")
    List<ReportSnapshotShard> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    @Select("SELECT * FROM report_snapshot_shard WHERE snapshot_id = #{snapshotId} AND bind_name = #{bindName} AND deleted = 0 ORDER BY shard_index")
    List<ReportSnapshotShard> selectBySnapshotAndBindName(
            @Param("snapshotId") Long snapshotId,
            @Param("bindName") String bindName);

    @Select("SELECT * FROM report_snapshot_shard WHERE snapshot_id = #{snapshotId} AND bind_name = #{bindName} " +
            "AND shard_index = #{shardIndex} AND deleted = 0 LIMIT 1")
    ReportSnapshotShard selectBySnapshotAndShardIndex(
            @Param("snapshotId") Long snapshotId,
            @Param("bindName") String bindName,
            @Param("shardIndex") Integer shardIndex);

    @Select("SELECT DISTINCT bind_name FROM report_snapshot_shard WHERE snapshot_id = #{snapshotId} AND deleted = 0")
    List<String> selectDistinctBindNames(@Param("snapshotId") Long snapshotId);

    @Delete("DELETE FROM report_snapshot_shard WHERE snapshot_id = #{snapshotId}")
    int deleteBySnapshotId(@Param("snapshotId") Long snapshotId);

    @Select("SELECT COUNT(*) FROM report_snapshot_shard WHERE snapshot_id = #{snapshotId} AND bind_name = #{bindName} AND deleted = 0")
    int countShardsBySnapshotAndBindName(
            @Param("snapshotId") Long snapshotId,
            @Param("bindName") String bindName);
}
