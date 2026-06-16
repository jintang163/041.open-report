import { get, post, put, del } from '@/utils/request'
import { DataSet, PageParams, PageResult } from '@/types'

export const getDatasetList = (params: PageParams): Promise<PageResult<DataSet>> => {
  return get('/dataset/list', params)
}

export const getDatasetAll = (): Promise<DataSet[]> => {
  return get('/dataset/all')
}

export const getDatasetById = (id: number): Promise<DataSet> => {
  return get(`/dataset/${id}`)
}

export const createDataset = (data: DataSet): Promise<DataSet> => {
  return post('/dataset', data)
}

export const updateDataset = (data: DataSet): Promise<DataSet> => {
  return put('/dataset', data)
}

export const deleteDataset = (id: number): Promise<void> => {
  return del(`/dataset/${id}`)
}

export const batchDeleteDataset = (ids: number[]): Promise<void> => {
  return post('/dataset/batch-delete', { ids })
}

export const testDataset = (id: number, params?: Record<string, any>): Promise<any[]> => {
  return post(`/dataset/test/${id}`, params)
}

export const executeDataset = (datasourceId: number, sql: string): Promise<any[]> => {
  return post('/dataset/execute', { datasourceId, sql })
}

export const getDatasetColumns = (id: number): Promise<{ name: string; type: string }[]> => {
  return get(`/dataset/columns/${id}`)
}
