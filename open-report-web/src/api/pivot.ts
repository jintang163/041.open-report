import { post } from '@/utils/request'
import type { PivotTableConfig, PivotTableResult, Result } from '@/types'

export const executePivot = (
  config: PivotTableConfig,
  params?: Record<string, any>
): Promise<Result<PivotTableResult>> => {
  return post('/pivot-table/execute', { config, params })
}

export const previewPivot = (
  config: PivotTableConfig,
  params?: Record<string, any>,
  limit?: number
): Promise<Result<any>> => {
  const query = limit !== undefined ? `?limit=${limit}` : ''
  return post(`/pivot-table/preview${query}`, { config, params })
}

export const generatePivotSql = (config: PivotTableConfig): Promise<Result<string>> => {
  return post('/pivot-table/generate-sql', { config })
}
