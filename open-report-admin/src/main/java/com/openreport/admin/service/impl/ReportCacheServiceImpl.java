package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.openreport.admin.entity.ReportCacheWarmupConfig;
import com.openreport.admin.service.ReportAccessLogService;
import com.openreport.admin.service.ReportCacheService;
import com.openreport.admin.service.ReportExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReportCacheServiceImpl implements ReportCacheService {

    private static final String CACHE_PREFIX = "report:cache:";
    private static final String CACHE_KEYS_SET = "report:cache:keys";
    private static final String DEFAULT_PARAMS_HASH = "default";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ReportExecuteService reportExecuteService;

    @Autowired
    private ReportAccessLogService reportAccessLogService;

    @Override
    public String getCacheKey(Long templateId, String paramsHash) {
        String hash = (paramsHash == null || paramsHash.isEmpty()) ? DEFAULT_PARAMS_HASH : paramsHash;
        return CACHE_PREFIX + templateId + ":" + hash;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedReport(Long templateId, String paramsHash) {
        String cacheKey = getCacheKey(templateId, paramsHash);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                Map<String, Object> result = (Map<String, Object>) cached;
                result.put("fromCache", true);
                result.put("cacheKey", cacheKey);
                return result;
            }
        } catch (Exception e) {
            log.warn("从Redis获取报表缓存失败: templateId={}, key={}", templateId, cacheKey, e);
        }
        return null;
    }

    @Override
    public void cacheReport(Long templateId, String paramsHash, Map<String, Object> data, long ttlSeconds) {
        String cacheKey = getCacheKey(templateId, paramsHash);
        try {
            Map<String, Object> cacheData = new LinkedHashMap<>(data);
            cacheData.remove("fromCache");
            cacheData.remove("cacheKey");

            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(cacheKey, cacheData, ttlSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(cacheKey, cacheData, 12, TimeUnit.HOURS);
            }
            redisTemplate.opsForSet().add(CACHE_KEYS_SET, cacheKey);
            log.debug("报表数据已缓存: templateId={}, key={}, ttl={}s", templateId, cacheKey, ttlSeconds);
        } catch (Exception e) {
            log.warn("缓存报表数据失败: templateId={}, key={}", templateId, cacheKey, e);
        }
    }

    @Override
    public String computeParamsHash(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return DEFAULT_PARAMS_HASH;
        }
        try {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Map) {
                    sorted.put(entry.getKey(), new TreeMap<>((Map<String, Object>) val));
                } else if (val instanceof Collection) {
                    List<?> list = new ArrayList<>((Collection<?>) val);
                    Collections.sort(list, (a, b) -> String.valueOf(a).compareTo(String.valueOf(b)));
                    sorted.put(entry.getKey(), list);
                } else {
                    sorted.put(entry.getKey(), val);
                }
            }
            String jsonStr = JSON.toJSONString(sorted);
            int hash = Math.abs(jsonStr.hashCode());
            return String.format("%08x", hash);
        } catch (Exception e) {
            log.warn("计算参数哈希失败", e);
            return DEFAULT_PARAMS_HASH;
        }
    }

    @Override
    public void evictCache(Long templateId) {
        String pattern = CACHE_PREFIX + templateId + ":*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                redisTemplate.opsForSet().remove(CACHE_KEYS_SET, keys.toArray());
                log.info("已清除报表模板所有缓存: templateId={}, 数量={}", templateId, keys.size());
            }
        } catch (Exception e) {
            log.warn("清除报表缓存失败: templateId={}", templateId, e);
        }
    }

    @Override
    public void evictCache(Long templateId, String paramsHash) {
        String cacheKey = getCacheKey(templateId, paramsHash);
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            redisTemplate.opsForSet().remove(CACHE_KEYS_SET, cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("已清除报表缓存: key={}", cacheKey);
            }
        } catch (Exception e) {
            log.warn("清除报表缓存失败: key={}", cacheKey, e);
        }
    }

    @Override
    public void evictAllCache() {
        try {
            Set<Object> keys = redisTemplate.opsForSet().members(CACHE_KEYS_SET);
            if (keys != null && !keys.isEmpty()) {
                List<String> keyList = new ArrayList<>();
                for (Object k : keys) {
                    keyList.add(String.valueOf(k));
                }
                redisTemplate.delete(keyList);
                redisTemplate.delete(CACHE_KEYS_SET);
                log.info("已清除所有报表缓存，数量={}", keyList.size());
            }
        } catch (Exception e) {
            log.warn("清除所有报表缓存失败", e);
        }
    }

    @Override
    public Map<String, Object> getCacheInfo(Long templateId) {
        Map<String, Object> info = new LinkedHashMap<>();
        String pattern = CACHE_PREFIX + templateId + ":*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            List<Map<String, Object>> cacheItems = new ArrayList<>();
            long totalSize = 0;
            if (keys != null) {
                for (String key : keys) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("key", key);
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    item.put("ttlSeconds", ttl);
                    try {
                        Object val = redisTemplate.opsForValue().get(key);
                        if (val != null) {
                            String json = JSON.toJSONString(val);
                            int size = json.getBytes().length;
                            totalSize += size;
                            item.put("sizeBytes", size);
                        }
                    } catch (Exception ignored) {}
                    cacheItems.add(item);
                }
            }
            info.put("templateId", templateId);
            info.put("cacheCount", cacheItems.size());
            info.put("totalSizeBytes", totalSize);
            info.put("totalSizeMB", String.format("%.2f", totalSize / 1024.0 / 1024.0));
            info.put("items", cacheItems);
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    @Override
    public Map<String, Object> getOverallCacheInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            Set<Object> keys = redisTemplate.opsForSet().members(CACHE_KEYS_SET);
            int count = 0;
            long totalSize = 0;
            if (keys != null) {
                count = keys.size();
                for (Object k : keys) {
                    try {
                        Object val = redisTemplate.opsForValue().get(String.valueOf(k));
                        if (val != null) {
                            totalSize += JSON.toJSONString(val).getBytes().length;
                        }
                    } catch (Exception ignored) {}
                }
            }
            info.put("cacheCount", count);
            info.put("totalSizeBytes", totalSize);
            info.put("totalSizeMB", String.format("%.2f", totalSize / 1024.0 / 1024.0));
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    @Override
    public Map<String, Object> warmupReport(Long templateId, Map<String, Object> defaultParams) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        long startTime = System.currentTimeMillis();
        try {
            ReportCacheWarmupConfig config = reportAccessLogService.getOrCreateDefaultConfig();
            long ttl = config.getCacheTtlSeconds() != null ? config.getCacheTtlSeconds() : 43200L;

            Map<String, Object> params = (defaultParams != null && !defaultParams.isEmpty())
                    ? defaultParams : parseDefaultParams(config.getDefaultParamsJson());

            Map<String, Object> reportData = reportExecuteService.executeReportInternal(templateId, params);
            Boolean success = (Boolean) reportData.get("success");
            if (Boolean.TRUE.equals(success)) {
                String paramsHash = computeParamsHash(params);
                cacheReport(templateId, paramsHash, reportData, ttl);
                reportAccessLogService.incrementWarmupCount(templateId, LocalDate.now());
                long elapsed = System.currentTimeMillis() - startTime;
                result.put("success", true);
                result.put("paramsHash", paramsHash);
                result.put("cacheTtlSeconds", ttl);
                result.put("elapsedMs", elapsed);
                result.put("message", "预热成功，耗时 " + elapsed + "ms");
                log.info("报表预热成功: templateId={}, 耗时={}ms", templateId, elapsed);
            } else {
                result.put("success", false);
                result.put("message", reportData.get("message") != null ? reportData.get("message") : "报表执行失败");
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "预热异常: " + e.getMessage());
            result.put("elapsedMs", elapsed);
            log.error("报表预热失败: templateId={}", templateId, e);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> warmupHotReports(Integer limit, Integer minAccessCount, Integer statsDays) {
        List<Map<String, Object>> results = new ArrayList<>();
        ReportCacheWarmupConfig config = reportAccessLogService.getOrCreateDefaultConfig();

        int useLimit = (limit != null && limit > 0) ? limit :
                (config.getMaxHotReports() != null ? config.getMaxHotReports() : 50);
        int useThreshold = (minAccessCount != null && minAccessCount > 0) ? minAccessCount :
                (config.getHotThreshold() != null ? config.getHotThreshold() : 50);
        int useDays = (statsDays != null && statsDays > 0) ? statsDays :
                (config.getStatsWindowDays() != null ? config.getStatsWindowDays() : 7);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(useDays - 1);

        List<Map<String, Object>> hotReports = reportAccessLogService.getHotReportsWithThreshold(
                startDate, endDate, useThreshold);
        if (hotReports == null || hotReports.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("info", "没有达到访问阈值的报表，阈值=" + useThreshold + ", 窗口=" + useDays + "天");
            results.add(empty);
            return results;
        }

        log.info("开始批量预热高频报表，候选数量={}, limit={}", hotReports.size(), useLimit);
        int count = 0;
        for (Map<String, Object> hot : hotReports) {
            if (count >= useLimit) break;
            try {
                Long templateId = Long.valueOf(String.valueOf(hot.get("template_id")));
                Object accessCountObj = hot.get("access_count");
                Map<String, Object> warmupResult = warmupReport(templateId, null);
                warmupResult.put("accessCount", accessCountObj);
                results.add(warmupResult);
                count++;
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("批量预热报表异常", e);
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("templateId", hot.get("template_id"));
                err.put("success", false);
                err.put("message", e.getMessage());
                results.add(err);
            }
        }
        log.info("批量高频报表预热完成，成功处理 {} 个", count);
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDefaultParams(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return JSON.parseObject(json, Map.class);
        } catch (Exception e) {
            log.warn("解析默认预热参数失败", e);
            return new HashMap<>();
        }
    }
}
