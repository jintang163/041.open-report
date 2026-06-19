import { get, post, put, del } from '@/utils/request'

export interface CacheWarmupConfig {
  id: number
  configName: string
  enabled: number
  hotThreshold: number
  statsWindowDays: number
  maxHotReports: number
  lowPeakStartHour: number
  lowPeakEndHour: number
  cacheTtlSeconds: number
  defaultParamsJson: string
  createTime: string
  updateTime: string
}

export interface CacheStats {
  id: number
  templateId: number
  templateName: string
  statsDate: string
  totalRequests: number
  cacheHits: number
  cacheMisses: number
  warmupCount: number
  avgResponseTimeMs: number
  createTime: string
  updateTime: string
}

export interface HotReportItem {
  template_id: number
  template_name: string
  access_count: number
}

export interface WarmupResult {
  templateId: number
  success: boolean
  message?: string
  paramsHash?: string
  cacheTtlSeconds?: number
  elapsedMs?: number
  accessCount?: number
}

export const getOverallCacheInfo = (): Promise<{
  cache: {
    cacheCount: number
    totalSizeBytes: number
    totalSizeMB: string
  }
  config: CacheWarmupConfig
}> => {
  return get('/report-cache/info')
}

export const getTemplateCacheInfo = (templateId: number): Promise<{
  templateId: number
  cacheCount: number
  totalSizeBytes: number
  totalSizeMB: string
  items: Array<{
    key: string
    ttlSeconds: number
    sizeBytes?: number
  }>
}> => {
  return get(`/report-cache/info/${templateId}`)
}

export const warmupSingleReport = (
  templateId: number,
  params?: Record<string, any>
): Promise<WarmupResult> => {
  return post(`/report-cache/warmup/${templateId}`, params)
}

export const warmupHotReports = (
  limit?: number,
  minAccessCount?: number,
  statsDays?: number
): Promise<WarmupResult[]> => {
  const query: Record<string, any> = {}
  if (limit !== undefined) query.limit = limit
  if (minAccessCount !== undefined) query.minAccessCount = minAccessCount
  if (statsDays !== undefined) query.statsDays = statsDays
  return post('/report-cache/warmup/hot', undefined, { params: query })
}

export const evictTemplateCache = (templateId: number): Promise<void> => {
  return del(`/report-cache/evict/${templateId}`)
}

export const evictAllCache = (): Promise<void> => {
  return del('/report-cache/evict/all')
}

export const cleanupExpiredCache = (): Promise<{
  checkedCount: number
  expiredCount: number
  freedBytes: number
  freedMB: string
  keptCount: number
  message: string
  removedKeys: string[]
}> => {
  return post('/report-cache/cleanup')
}

export const getHotReports = (
  days: number = 7,
  limit: number = 20
): Promise<HotReportItem[]> => {
  return get('/report-cache/hot-reports', { params: { days, limit } })
}

export const getHotParamCombos = (
  days: number = 7,
  limit: number = 50,
  threshold: number = 20
): Promise<Array<{
  template_id: number
  params_hash: string
  access_count: number
}>> => {
  return get('/report-cache/hot-param-combos', { params: { days, limit, threshold } })
}

export const warmupHotParamCombos = (
  limit?: number,
  minAccessCount?: number,
  statsDays?: number
): Promise<WarmupResult[]> => {
  const query: Record<string, any> = {}
  if (limit !== undefined) query.limit = limit
  if (minAccessCount !== undefined) query.minAccessCount = minAccessCount
  if (statsDays !== undefined) query.statsDays = statsDays
  return post('/report-cache/warmup/hot-param-combos', undefined, { params: query })
}

export const getOverallStats = (
  startDate: string,
  endDate: string
): Promise<{
  startDate: string
  endDate: string
  days: number
  totalRequests: number
  totalCacheHits: number
  totalCacheMisses: number
  hitRate: string
  avgResponseTimeMs: number
  daily: Array<Record<string, any>>
}> => {
  return get('/report-cache/stats/overall', { params: { startDate, endDate } })
}

export const getDailyStatsByTemplate = (date: string): Promise<Array<Record<string, any>>> => {
  return get('/report-cache/stats/daily', { params: { date } })
}

export const getAggregatedStats = (
  startDate: string,
  endDate: string
): Promise<CacheStats[]> => {
  return get('/report-cache/stats/aggregated', { params: { startDate, endDate } })
}

export const aggregateStats = (date: string): Promise<void> => {
  return post('/report-cache/stats/aggregate', undefined, { params: { date } })
}

export const getWarmupConfig = (): Promise<CacheWarmupConfig> => {
  return get('/report-cache/config')
}

export const updateWarmupConfig = (
  config: Partial<CacheWarmupConfig>
): Promise<CacheWarmupConfig> => {
  return put('/report-cache/config', config)
}
