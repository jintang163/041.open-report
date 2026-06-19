package com.openreport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openreport.admin.entity.ReportCacheWarmupConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportCacheWarmupConfigMapper extends BaseMapper<ReportCacheWarmupConfig> {

    @Select("SELECT * FROM report_cache_warmup_config WHERE enabled = 1 AND deleted = 0 ORDER BY id LIMIT 1")
    ReportCacheWarmupConfig selectActiveConfig();
}
