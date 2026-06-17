import { get, post, put, del } from '@/utils/request'
import { DataSourceConfig, PageParams, PageResult } from '@/types'

export const getDatasourceList = (params: PageParams): Promise<PageResult<DataSourceConfig>> => {
  return get('/datasource/list', params)
}

export const getDatasourceAll = (): Promise<DataSourceConfig[]> => {
  return get('/datasource/all')
}

export const getDatasourceById = (id: number): Promise<DataSourceConfig> => {
  return get(`/datasource/${id}`)
}

export const createDatasource = (data: DataSourceConfig): Promise<DataSourceConfig> => {
  return post('/datasource', data)
}

export const updateDatasource = (data: DataSourceConfig): Promise<DataSourceConfig> => {
  return put('/datasource', data)
}

export const deleteDatasource = (id: number): Promise<void> => {
  return del(`/datasource/${id}`)
}

export const batchDeleteDatasource = (ids: number[]): Promise<void> => {
  return post('/datasource/batch-delete', { ids })
}

export const testConnection = (data: DataSourceConfig): Promise<{ success: boolean; message: string }> => {
  return post('/datasource/test-connection', data)
}

export const getDatasourceTables = (id: number): Promise<Array<{
  tableName: string
  tableType: string
  remarks?: string
}>> => {
  return get(`/datasource/${id}/tables`)
}

export const getTableColumns = (id: number, tableName: string): Promise<Array<{
  columnName: string
  dataType: string
  columnSize?: number
  nullable?: boolean
  remarks?: string
}>> => {
  return get(`/datasource/${id}/columns/${tableName}`)
}

export const getDatasourceSchema = (id: number): Promise<string> => {
  return get(`/datasource/${id}/schema`)
}

export const validateDatasourceSql = (id: number, sql: string): Promise<{
  success: boolean
  message: string
  duration?: number
  columns?: Array<{ name: string; type: string }>
  sampleData?: Record<string, any>[]
}> => {
  return post(`/datasource/${id}/validate-sql`, { sql })
}
