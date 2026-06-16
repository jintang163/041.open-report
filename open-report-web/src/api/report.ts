import { get, post, put, del } from '@/utils/request'
import { ReportTemplate, PageParams, PageResult } from '@/types'

export const getReportList = (params: PageParams): Promise<PageResult<ReportTemplate>> => {
  return get('/report/list', params)
}

export const getReportAll = (): Promise<ReportTemplate[]> => {
  return get('/report/all')
}

export const getReportById = (id: number): Promise<ReportTemplate> => {
  return get(`/report/${id}`)
}

export const createReport = (data: ReportTemplate): Promise<ReportTemplate> => {
  return post('/report', data)
}

export const updateReport = (data: ReportTemplate): Promise<ReportTemplate> => {
  return put('/report', data)
}

export const deleteReport = (id: number): Promise<void> => {
  return del(`/report/${id}`)
}

export const batchDeleteReport = (ids: number[]): Promise<void> => {
  return post('/report/batch-delete', { ids })
}

export const copyReport = (id: number): Promise<ReportTemplate> => {
  return post(`/report/copy/${id}`)
}

export const executeReport = (id: number, params?: Record<string, any>): Promise<{
  html?: string
  data?: any
  charts?: any[]
}> => {
  return post(`/report/execute/${id}`, params)
}

export const exportReportExcel = (id: number, params?: Record<string, any>): Promise<Blob> => {
  return post(`/report/export/${id}/excel`, params, { responseType: 'blob' })
}

export const exportReportPdf = (id: number, params?: Record<string, any>): Promise<Blob> => {
  return post(`/report/export/${id}/pdf`, params, { responseType: 'blob' })
}

export const getReportParameters = (id: number): Promise<any[]> => {
  return get(`/report/parameters/${id}`)
}

export const publishReport = (id: number): Promise<void> => {
  return post(`/report/publish/${id}`)
}

export const unpublishReport = (id: number): Promise<void> => {
  return post(`/report/unpublish/${id}`)
}

export const importReport = (data: FormData): Promise<ReportTemplate> => {
  return post('/report/import', data, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export const exportReport = (id: number): Promise<Blob> => {
  return get(`/report/export/${id}`, undefined, { responseType: 'blob' })
}

export const getPublicReportInfo = (token: string): Promise<{
  id: number
  name: string
  params?: any[]
}> => {
  return get(`/report/public/${token}`)
}

export const executePublicReport = (token: string, params?: Record<string, any>): Promise<{
  html?: string
  data?: any
  charts?: any[]
}> => {
  return post(`/report/public/execute/${token}`, params)
}

export const exportPublicReportExcel = (token: string, params?: Record<string, any>): Promise<Blob> => {
  return post(`/report/public/export/${token}/excel`, params, { responseType: 'blob' })
}

export const exportPublicReportPdf = (token: string, params?: Record<string, any>): Promise<Blob> => {
  return post(`/report/public/export/${token}/pdf`, params, { responseType: 'blob' })
}
