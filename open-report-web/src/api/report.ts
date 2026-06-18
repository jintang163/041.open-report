import { get, post, put, del } from '@/utils/request'
import { ReportTemplate, PageParams, PageResult, ReportTemplateSnapshot, ReportApproval, TemplateVersionDiffDTO } from '@/types'

export const getReportList = (params: PageParams): Promise<PageResult<ReportTemplate>> => {
  return get('/report-template/page', params)
}

export const getReportListV2 = (params: PageParams & { status?: number }): Promise<PageResult<ReportTemplate>> => {
  return get('/report-template/page-v2', params)
}

export const getReportAll = (): Promise<ReportTemplate[]> => {
  return get('/report-template/list')
}

export const getReportById = (id: number): Promise<ReportTemplate> => {
  return get(`/report-template/${id}`)
}

export const createReport = (data: Partial<ReportTemplate>): Promise<ReportTemplate> => {
  return post('/report-template', data)
}

export const saveDraftReport = (data: Partial<ReportTemplate>): Promise<ReportTemplate> => {
  return post('/report-template/save-draft', data)
}

export const updateReport = (data: Partial<ReportTemplate>): Promise<ReportTemplate> => {
  return put('/report-template', data)
}

export const deleteReport = (id: number): Promise<void> => {
  return del(`/report-template/${id}`)
}

export const batchDeleteReport = (ids: number[]): Promise<void> => {
  return post('/report/batch-delete', { ids })
}

export const copyReport = (id: number): Promise<ReportTemplate> => {
  return post(`/report-template/copy/${id}`)
}

export const submitApproval = (id: number, remark?: string): Promise<ReportApproval> => {
  return post(`/report-template/submit-approval/${id}`, undefined, { params: { remark } })
}

export const getVersionList = (id: number): Promise<ReportTemplateSnapshot[]> => {
  return get(`/report-template/${id}/versions`)
}

export const getVersionDetail = (id: number, version: number): Promise<ReportTemplateSnapshot> => {
  return get(`/report-template/${id}/versions/${version}`)
}

export const compareVersions = (id: number, baseVersion: number, targetVersion: number): Promise<TemplateVersionDiffDTO> => {
  return get(`/report-template/${id}/versions/compare`, { baseVersion, targetVersion })
}

export const rollbackToVersion = (id: number, version: number): Promise<ReportTemplateSnapshot> => {
  return post(`/report-template/${id}/versions/rollback/${version}`)
}

export const getLatestPublishedVersion = (id: number): Promise<ReportTemplateSnapshot> => {
  return get(`/report-template/${id}/versions/latest-published`)
}

export const previewPublish = (id: number): Promise<ReportTemplateSnapshot> => {
  return get(`/report-template/${id}/preview-publish`)
}

export const getApprovalList = (params: PageParams & { status?: number }): Promise<PageResult<ReportApproval>> => {
  return get('/report-approval/page', params)
}

export const getApprovalByTemplateId = (templateId: number): Promise<ReportApproval[]> => {
  return get(`/report-approval/template/${templateId}`)
}

export const getApprovalById = (id: number): Promise<ReportApproval> => {
  return get(`/report-approval/${id}`)
}

export const approveApproval = (id: number, remark?: string): Promise<ReportApproval> => {
  return post(`/report-approval/${id}/approve`, undefined, { params: { remark } })
}

export const rejectApproval = (id: number, remark?: string): Promise<ReportApproval> => {
  return post(`/report-approval/${id}/reject`, undefined, { params: { remark } })
}

export const cancelApproval = (id: number): Promise<ReportApproval> => {
  return post(`/report-approval/${id}/cancel`)
}

export const executeReport = (id: number, params?: Record<string, any>): Promise<{
  html?: string
  data?: any
  charts?: any[]
}> => {
  return post(`/report/execute/${id}`, params)
}

export const getReportDataPage = (id: number, params?: Record<string, any>, pageNum = 1, pageSize = 100, dataSetId?: string): Promise<{
  columns?: any[]
  rows?: any[]
  total?: number
  pageNum?: number
  pageSize?: number
  hasMore?: boolean
  success?: boolean
}> => {
  return post(`/report-execute/report-data-page/${id}`, params, {
    params: { pageNum, pageSize, dataSetId }
  })
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
