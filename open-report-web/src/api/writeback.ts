import { get, post, put, del } from '@/utils/request'
import type {
  WritebackConfig,
  WritebackHistory,
  WritebackDetail,
  DataSubmitRequest,
  DataSubmitResult,
  PageParams,
  PageResult
} from '@/types'

export const getWritebackConfigList = (reportId: number): Promise<WritebackConfig[]> => {
  return get(`/report-writeback/config/list/${reportId}`)
}

export const getWritebackConfigDetail = (id: number): Promise<WritebackConfig> => {
  return get(`/report-writeback/config/${id}`)
}

export const createWritebackConfig = (data: Omit<WritebackConfig, 'id'>): Promise<void> => {
  return post('/report-writeback/config', data)
}

export const updateWritebackConfig = (data: WritebackConfig): Promise<void> => {
  return put('/report-writeback/config', data)
}

export const deleteWritebackConfig = (id: number): Promise<void> => {
  return del(`/report-writeback/config/${id}`)
}

export const submitReportData = (data: DataSubmitRequest): Promise<DataSubmitResult> => {
  return post('/report-writeback/submit', data)
}

export const getWritebackHistoryList = (reportId: number, pageNum: number = 1, pageSize: number = 100): Promise<PageResult<WritebackHistory>> => {
  return get('/report-writeback/history/page', { reportId, pageNum, pageSize })
}

export const getWritebackHistoryPage = (params: PageParams & { reportId: number }): Promise<PageResult<WritebackHistory>> => {
  return get('/report-writeback/history/page', params)
}

export const getWritebackHistoryDetail = (id: number): Promise<WritebackHistory> => {
  return get(`/report-writeback/history/${id}`)
}

export const getWritebackDetail = (historyId: number): Promise<WritebackDetail[]> => {
  return get(`/report-writeback/history/${historyId}/details`)
}

export const getWritebackHistoryDetails = (historyId: number): Promise<WritebackDetail[]> => {
  return getWritebackDetail(historyId)
}
