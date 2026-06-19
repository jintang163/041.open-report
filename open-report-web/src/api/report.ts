import { get, post, put, del } from '@/utils/request'
import { ReportTemplate, PageParams, PageResult, ReportTemplateSnapshot, ReportApproval, TemplateVersionDiffDTO, ReportSnapshotConfig, ReportDataSnapshot, SnapshotComparisonResult, SnapshotPageData, SnapshotStorageInfo, ReportSnapshotShard } from '@/types'

export const executeReportWithSnapshot = (
  id: number,
  params?: Record<string, any>,
  snapshotMode?: 'realtime' | 'latest' | 'snapshot',
  snapshotId?: number
): Promise<{
  html?: string
  data?: any
  charts?: any[]
  isSnapshot?: boolean
  snapshotId?: number
  snapshotName?: string
  snapshotTime?: string
}> => {
  const queryParams: Record<string, any> = {}
  if (snapshotMode) queryParams.snapshotMode = snapshotMode
  if (snapshotId) queryParams.snapshotId = snapshotId
  return post(`/report/execute/${id}`, params, { params: queryParams })
}

export const getSnapshotConfigByReportId = (reportId: number): Promise<ReportSnapshotConfig> => {
  return get(`/report-snapshot/config/report/${reportId}`)
}

export const createSnapshotConfig = (data: Partial<ReportSnapshotConfig>): Promise<ReportSnapshotConfig> => {
  return post('/report-snapshot/config', data)
}

export const updateSnapshotConfig = (data: Partial<ReportSnapshotConfig>): Promise<ReportSnapshotConfig> => {
  return put('/report-snapshot/config', data)
}

export const deleteSnapshotConfig = (id: number): Promise<void> => {
  return del(`/report-snapshot/config/${id}`)
}

export const toggleSnapshotConfig = (id: number, enabled: number): Promise<boolean> => {
  return put(`/report-snapshot/config/${id}/toggle`, undefined, { params: { enabled } })
}

export const createSnapshotManual = (configId: number, params?: Record<string, any>): Promise<{
  success: boolean
  snapshotId?: number
  snapshotName?: string
  rowCount?: number
  dataSize?: number
  executeTime?: number
}> => {
  return post(`/report-snapshot/create/${configId}`, params)
}

export const getSnapshotListByReportId = (reportId: number, limit = 50): Promise<ReportDataSnapshot[]> => {
  return get(`/report-snapshot/data/list/${reportId}`, { limit })
}

export const getLatestSnapshot = (reportId: number): Promise<ReportDataSnapshot> => {
  return get(`/report-snapshot/data/latest/${reportId}`)
}

export const loadSnapshotData = (snapshotId: number): Promise<Record<string, any>> => {
  return get(`/report-snapshot/data/${snapshotId}`)
}

export const deleteSnapshot = (snapshotId: number): Promise<boolean> => {
  return del(`/report-snapshot/data/${snapshotId}`)
}

export const compareSnapshots = (
  baseSnapshotId: number,
  targetSnapshotId: number
): Promise<SnapshotComparisonResult> => {
  return get('/report-snapshot/compare', { baseSnapshotId, targetSnapshotId })
}

export const compareSnapshotWithRealtime = (
  snapshotId: number,
  params?: Record<string, any>
): Promise<SnapshotComparisonResult> => {
  return post(`/report-snapshot/compare-realtime/${snapshotId}`, params)
}

export const getSnapshotDataPage = (
  snapshotId: number,
  params?: {
    bindName?: string
    pageNum?: number
    pageSize?: number
  }
): Promise<SnapshotPageData> => {
  return get(`/report-snapshot/data/page/${snapshotId}`, params)
}

export const getSnapshotStorageInfo = (snapshotId: number): Promise<SnapshotStorageInfo> => {
  return get(`/report-snapshot/data/storage-info/${snapshotId}`)
}

export const getSnapshotBindNames = (snapshotId: number): Promise<string[]> => {
  return get(`/report-snapshot/data/bind-names/${snapshotId}`)
}

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
  return post(`/report/report-data-page/${id}`, params, {
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

export const getPublicReportInfo = (token: string, password?: string): Promise<{
  id: number
  name: string
  description?: string
  params?: any[]
}> => {
  return get(`/report/public/${token}`, password ? { password } : undefined)
}

export const getPublicReportParameters = (token: string, password?: string): Promise<any[]> => {
  return get(`/report/public/params/${token}`, password ? { password } : undefined)
}

export const executePublicReport = (token: string, params?: Record<string, any>, password?: string): Promise<{
  html?: string
  data?: any
  charts?: any[]
}> => {
  return post(`/report/public/execute/${token}`, params, password ? { params: { password } } : undefined)
}

export const exportPublicReportExcel = (token: string, params?: Record<string, any>, password?: string): Promise<Blob> => {
  const queryParams = new URLSearchParams()
  if (password) queryParams.set('password', password)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      queryParams.set(key, String(value))
    })
  }
  return get(`/report/public/export/${token}/excel?${queryParams.toString()}`, undefined, { responseType: 'blob' })
}

export const exportPublicReportPdf = (token: string, params?: Record<string, any>, password?: string): Promise<Blob> => {
  const queryParams = new URLSearchParams()
  if (password) queryParams.set('password', password)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      queryParams.set(key, String(value))
    })
  }
  return get(`/report/public/export/${token}/pdf?${queryParams.toString()}`, undefined, { responseType: 'blob' })
}

export const generateShareLink = (id: number, expireSeconds?: number, password?: string): Promise<{
  reportId: number
  reportName: string
  shareToken: string
  shareUrl: string
  expireTime: string
  expireSeconds: number
  hasPassword: boolean
}> => {
  const params: Record<string, any> = {}
  if (expireSeconds) params.expireSeconds = expireSeconds
  if (password) params.password = password
  return post(`/report/public/${id}/generate`, undefined, { params })
}

export const cancelShare = (id: number): Promise<void> => {
  return post(`/report/public/${id}/cancel`)
}

export const getShareStatus = (id: number): Promise<{
  reportId: number
  shareEnabled: boolean
  shareToken: string
  shareUrl: string
  shareExpireTime: string
  hasPassword: boolean
  shareViewCount: number
}> => {
  return get(`/report/public/${id}/status`)
}
