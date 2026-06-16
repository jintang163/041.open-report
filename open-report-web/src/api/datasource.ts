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
