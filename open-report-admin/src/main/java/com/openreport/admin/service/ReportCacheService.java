package com.openreport.admin.service;

import java.util.List;
import java.util.Map;

public interface ReportCacheService {

    String getCacheKey(Long templateId, String paramsHash);

    Map<String, Object> getCachedReport(Long templateId, String paramsHash);

    void cacheReport(Long templateId, String paramsHash, Map<String, Object> data, long ttlSeconds);

    void saveParamsMapping(Long templateId, String paramsHash, Map<String, Object> params);

    Map<String, Object> getCachedParams(Long templateId, String paramsHash);

    String computeParamsHash(Map<String, Object> params);

    void evictCache(Long templateId);

    void evictCache(Long templateId, String paramsHash);

    void evictAllCache();

    Map<String, Object> getCacheInfo(Long templateId);

    Map<String, Object> getOverallCacheInfo();

    Map<String, Object> cleanupExpiredCache();

    Map<String, Object> warmupReport(Long templateId, Map<String, Object> defaultParams);

    List<Map<String, Object>> warmupHotReports(Integer limit, Integer minAccessCount, Integer statsDays);

    List<Map<String, Object>> warmupHotParamCombos(Integer limit, Integer minAccessCount, Integer statsDays);
}
