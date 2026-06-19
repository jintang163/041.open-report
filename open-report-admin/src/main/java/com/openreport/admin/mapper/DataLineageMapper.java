package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.DataLineage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface DataLineageMapper extends BaseMapper<DataLineage> {

    @Select("SELECT * FROM data_lineage WHERE report_id = #{reportId} AND deleted = 0 ORDER BY id")
    List<DataLineage> selectByReportId(@Param("reportId") Long reportId);

    @Select("SELECT * FROM data_lineage WHERE report_id = #{reportId} AND report_field = #{reportField} AND deleted = 0 LIMIT 1")
    DataLineage selectByReportField(@Param("reportId") Long reportId, @Param("reportField") String reportField);

    @Select("SELECT * FROM data_lineage WHERE data_set_id = #{dataSetId} AND deleted = 0 ORDER BY id")
    List<DataLineage> selectByDataSetId(@Param("dataSetId") Long dataSetId);

    @Select("SELECT * FROM data_lineage WHERE datasource_id = #{datasourceId} AND deleted = 0 ORDER BY id")
    List<DataLineage> selectByDatasourceId(@Param("datasourceId") Long datasourceId);

    @Select("SELECT * FROM data_lineage WHERE datasource_id = #{datasourceId} " +
            "AND table_name = #{tableName} AND deleted = 0 ORDER BY id")
    List<DataLineage> selectByTable(
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName);

    @Select("SELECT * FROM data_lineage WHERE datasource_id = #{datasourceId} " +
            "AND table_name = #{tableName} AND column_name = #{columnName} AND deleted = 0 ORDER BY id")
    List<DataLineage> selectByTableColumn(
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName,
            @Param("columnName") String columnName);

    @Select("SELECT DISTINCT report_id, report_name FROM data_lineage " +
            "WHERE datasource_id = #{datasourceId} AND table_name = #{tableName} " +
            "AND (column_name = #{columnName} OR #{columnName} IS NULL) AND deleted = 0")
    List<DataLineage> selectAffectedReports(
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName,
            @Param("columnName") String columnName);

    @Select("SELECT DISTINCT data_set_id, data_set_name FROM data_lineage " +
            "WHERE datasource_id = #{datasourceId} AND table_name = #{tableName} " +
            "AND (column_name = #{columnName} OR #{columnName} IS NULL) AND deleted = 0")
    List<DataLineage> selectAffectedDataSets(
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName,
            @Param("columnName") String columnName);

    @Select("SELECT COUNT(*) FROM data_lineage WHERE lineage_hash = #{hash} AND deleted = 0")
    int countByLineageHash(@Param("hash") String hash);

    @Delete("DELETE FROM data_lineage WHERE report_id = #{reportId}")
    int deleteByReportId(@Param("reportId") Long reportId);

    @Delete("DELETE FROM data_lineage WHERE data_set_id = #{dataSetId}")
    int deleteByDataSetId(@Param("dataSetId") Long dataSetId);
}
